/*
 *     AIS-catcher for Android
 *     Copyright (C)  2022 jvde.github@gmail.com.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

#include <jni.h>
#include <android/log.h>

#include <vector>
#include <string>
#include <sstream>

#include <AIS-catcher.h>

#define LOG_TAG "AIS-catcher JNI"

#define LOGE(...) \
  __android_log_print(ANDROID_LO \
  G_ERROR, LOG_TAG, __VA_ARGS__)

#include "AIS-catcher.h"

#include "Signals.h"
#include "Common.h"
#include "Model.h"
#include "IO.h"

#include "Device/RTLSDR.h"
#include "Device/AIRSPYHF.h"
#include "Device/HACKRF.h"
#include "Device/RTLTCP.h"
#include "Device/AIRSPY.h"

static JavaVM *javaVm = nullptr;
static int javaVersion;
static jclass javaClass = nullptr;

struct Statistics
{
    int DataB;
    int DataGB;
    int Total;
    int ChA;
    int ChB;
    int Msg[28];
    int Error;

} statistics;

std::string nmea_msg;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *, void *)
{
    return JNI_VERSION_1_6;
}

/*
void Attach(JNIEnv *env)
{
    if (javaVm->GetEnv((void **) &env, javaVersion) == JNI_EDETACHED)
       javaVm->AttachCurrentThread(&env, nullptr);
}

void DetachThread()
{
    javaVm->DetachCurrentThread();
}
*/

// JAVA interaction and callbacks

void pushStatistics(JNIEnv *env) {

    env->SetStaticIntField(javaClass, env->GetStaticFieldID(javaClass, "Statistics_Data", "I"), statistics.DataB / 1000000);
    env->SetStaticIntField(javaClass, env->GetStaticFieldID(javaClass, "Statistics_Total", "I"), statistics.Total);
    env->SetStaticIntField(javaClass, env->GetStaticFieldID(javaClass, "Statistics_ChA", "I"), statistics.ChA);
    env->SetStaticIntField(javaClass, env->GetStaticFieldID(javaClass, "Statistics_ChB", "I"), statistics.ChB);
    env->SetStaticIntField(javaClass, env->GetStaticFieldID(javaClass, "Statistics_Msg123", "I"), statistics.Msg[1] + statistics.Msg[2] + statistics.Msg[3]);
    env->SetStaticIntField(javaClass, env->GetStaticFieldID(javaClass, "Statistics_Msg5", "I"), statistics.Msg[5]);
    env->SetStaticIntField(javaClass, env->GetStaticFieldID(javaClass, "Statistics_Msg1819", "I"), statistics.Msg[18] + statistics.Msg[19]);
    env->SetStaticIntField(javaClass, env->GetStaticFieldID(javaClass, "Statistics_Msg24", "I"), statistics.Msg[24] + statistics.Msg[25]);
    env->SetStaticIntField(javaClass, env->GetStaticFieldID(javaClass, "Statistics_MsgOther", "I"), statistics.Total - (statistics.Msg[1] + statistics.Msg[2] + statistics.Msg[3] + statistics.Msg[5] + statistics.Msg[18] + statistics.Msg[19] + statistics.Msg[24] + statistics.Msg[25]));
}


static void callbackNMEA(JNIEnv *env,const std::string& str) {

    jstring jstr = env->NewStringUTF(str.c_str());
    jmethodID method = env->GetStaticMethodID(javaClass, "callbackNMEA", "(Ljava/lang/String;)V");
    env->CallStaticVoidMethod(javaClass, method, jstr);
}

static void callbackClose(JNIEnv *env) {

    jmethodID method = env->GetStaticMethodID(javaClass, "callbackClose", "()V");
    env->CallStaticVoidMethod(javaClass, method);
}

static void callbackConsole(JNIEnv *env,const std::string& str) {

    jstring jstr = env->NewStringUTF(str.c_str());
    jmethodID method = env->GetStaticMethodID(javaClass, "callbackConsole", "(Ljava/lang/String;)V");
    env->CallStaticVoidMethod(javaClass, method, jstr);
}

static void callbackConsoleFormat(JNIEnv *env,const char *format, ...) {

    char buffer[256];
    va_list args;
    va_start (args, format);
    vsnprintf (buffer, 255, format, args);

    jstring jstr = env->NewStringUTF(buffer);
    jmethodID method = env->GetStaticMethodID(javaClass, "callbackConsole", "(Ljava/lang/String;)V");
    env->CallStaticVoidMethod(javaClass, method, jstr);

    va_end (args);
}

static void callbackUpdate(JNIEnv *env) {

    pushStatistics(env);

    jmethodID method = env->GetStaticMethodID(javaClass, "callbackUpdate", "()V");
    env->CallStaticVoidMethod(javaClass, method);
}

static void callbackError(JNIEnv *env,const std::string& str) {

    callbackConsole(env,str);
    callbackClose(env);

    jstring jstr = env->NewStringUTF(str.c_str());
    jmethodID method = env->GetStaticMethodID(javaClass, "callbackError", "(Ljava/lang/String;)V");
    env->CallStaticVoidMethod(javaClass, method, jstr);
}

// AIS-catcher model

class NMEAcounter : public StreamIn<NMEA>
{
    std::string list;
    bool clean = true;

public:

    void Receive(const NMEA* data, int len) {
        std::string str;

        for (int i = 0; i < len; i++) {
            for (const auto &s: data[i].sentence) {
                str.append("\n" + s);
            }

            statistics.Total++;

            if (data[i].channel == 'A')
                statistics.ChA++;
            else
                statistics.ChB++;

            int msg = data[i].msg;

            if (msg > 27 || msg < 1) statistics.Error++;
            if (msg <= 27) statistics.Msg[msg]++;

            nmea_msg += str;
        }
    }
};

// Counting Data received from device
class RAWcounter : public StreamIn<RAW>
{
public:

    void Receive(const RAW* data, int len)
    {
        const int GB = 1000000000;
        statistics.DataB += data->size;
        statistics.DataGB += statistics.DataB / GB ;
        statistics.DataB %= GB;
    }
};

struct Drivers
{
    Device::RTLSDR RTLSDR;
    Device::RTLTCP RTLTCP;
    Device::AIRSPY AIRSPY;
    Device::AIRSPYHF AIRSPYHF;
} drivers;

//Device::Type type = Device::Type::NONE;
std::vector<IO::UDP> UDP_connections;

NMEAcounter NMEAcounter;
RAWcounter rawcounter;

Device::Device *device = nullptr;
AIS::Model *model = nullptr;

bool stop = false;

extern "C"
JNIEXPORT jint JNICALL Java_com_jvdegithub_aiscatcher_AisCatcherJava_Init(JNIEnv *env, jclass instance) {

    env->GetJavaVM(&javaVm);
    javaVersion = env->GetVersion();
    javaClass = (jclass) env->NewGlobalRef(instance);

    callbackConsole(env, "AIS-Catcher " VERSION "\n");
    memset(&statistics, 0, sizeof(statistics));

    return 0;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_isStreaming(JNIEnv *, jclass ) {
    if(device && device->isStreaming()) return JNI_TRUE;

    return JNI_FALSE;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_applySetting(JNIEnv *env, jclass, jstring dev,
                                                           jstring setting, jstring param) {

    try{
        jboolean isCopy;
        std::string d = (env)->GetStringUTFChars(dev, &isCopy);
        std::string s = (env)->GetStringUTFChars(setting, &isCopy);
        std::string p = (env)->GetStringUTFChars(param, &isCopy);

        switch(d[0]) {
            case 't':
                callbackConsoleFormat(env,"Set RTLTCP: [%s] %s\n",s.c_str(),p.c_str());
                drivers.RTLTCP.Set(s, p);
                break;
            case 'r':
                callbackConsoleFormat(env,"Set RTLSDR: [%s] %s\n",s.c_str(),p.c_str());
                drivers.RTLSDR.Set(s, p);
                break;
            case 'm':
                callbackConsoleFormat(env,"Set AIRSPY: [%s] %s\n",s.c_str(),p.c_str());
                drivers.AIRSPY.Set(s, p);
                break;
            case 'h':
                callbackConsoleFormat(env,"Set AIRSPYHF: [%s] %s\n",s.c_str(),p.c_str());
                drivers.AIRSPYHF.Set(s, p);
                break;

        }

    } catch (const char *msg) {
        callbackError(env,msg);
        device = nullptr;
        return -1;
    }
    return 0;
}


extern "C"
JNIEXPORT jint JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_Run(JNIEnv *env, jclass ) {


    try {
        callbackConsole(env,"Starting device\n");
        device->Play();
        stop = false;

        callbackConsole(env,"Run started\n");

        while (device->isStreaming() && !stop) {
            std::this_thread::sleep_for(std::chrono::milliseconds(1000));
            callbackUpdate(env);
            if(!nmea_msg.empty())
            {
                callbackNMEA(env, nmea_msg);
                nmea_msg = "";
            }
        }

        device->Stop();
    }
    catch (const char *msg) {
        callbackError(env,msg);
        return -1;
    }

    if(!stop) {
        callbackError(env,"Device disconnected");
    }

    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_Close(JNIEnv *env, jclass ) {
    callbackConsole(env,"Device closing\n");

    try {
        if(device) device->Close();
        device = nullptr;
        UDP_connections.clear();
        delete model; model = nullptr;
        UDP_connections.resize(0);
    }
    catch (const char *msg) {
        callbackError(env,msg);
        return -1;
    }
    callbackClose(env);
    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_forceStop(JNIEnv *env, jclass ) {
    callbackConsole(env,"Stop requested\n");
    stop = true;
    return 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_resetStatistics(JNIEnv *env, jclass ) {

    memset(&statistics,0, sizeof(statistics));

    callbackUpdate(env);
    callbackConsole(env,"");
    callbackNMEA(env,"");
}


extern "C"
JNIEXPORT jint JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_createReceiver(JNIEnv *env, jclass , jint source,
                                                             jint fd) {
    callbackConsole(env, "Creating Receiver\n");

    if (device != nullptr) {
        callbackConsole(env, "Error: device already assigned.");
        return -1;
    }

    if (source == 0) {
        callbackConsole(env, "Device : RTLTCP\n");
        device = &drivers.RTLTCP;
    } else if (source == 1) {
        callbackConsole(env, "Device : RTLSDR\n");
        device = &drivers.RTLSDR;
    } else if (source == 2) {
        callbackConsole(env, "Device : AIRSPY\n");
        device = &drivers.AIRSPY;
    } else if (source == 3) {
        callbackConsole(env, "Device : AIRSPYHF\n");
        device = &drivers.AIRSPYHF;
    } else {
        callbackConsole(env, "Support for this device not included.");
        return -1;
    }

    try {
        device->out.Clear();
        device->OpenWithFileDescriptor(fd);
        device->setFrequency(162000000);
    }
    catch (const char *msg) {
        callbackConsole(env,msg);
        device = nullptr;
        return -1;
    }

    callbackConsole(env,"Creating Model\n");
    callbackConsoleFormat(env,"Building model with sampling rate: %dK\n",device->getSampleRate()/1000);

    delete model;
    model = new AIS::ModelDefault();
    model->buildModel(device->getSampleRate(), false, device);

    callbackConsole(env,"Creating output channels\n");

    try{

        for(auto & UDP_connection : UDP_connections) {
            model->Output() >> UDP_connection;
        }
    } catch (const char *msg) {
        callbackError(env,msg);
        device = nullptr;
        return -1;
    }

    device->out >> rawcounter;
    model->Output() >> NMEAcounter;

    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_createUDP(JNIEnv *env, jclass clazz, jstring h,
                                                        jstring p) {
    try {
        UDP_connections.resize(UDP_connections.size()+1);

        jboolean b;
        std::string host = (env)->GetStringUTFChars(h, &b);
        std::string port = (env)->GetStringUTFChars(p, &b);
        UDP_connections[UDP_connections.size()-1].openConnection(host,port);

        callbackConsoleFormat(env,"UDP: %s %s\n",host.c_str(),port.c_str());


    } catch (const char *msg) {
        callbackError(env,msg);
        device = nullptr;
        return -1;
    }
    return 0;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_getSampleRate(JNIEnv *env, jclass clazz) {
    if(device == NULL) return 0;

    return device->getSampleRate();
}