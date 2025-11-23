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

const int TIME_CONSTRAINT = 120;
bool communityFeed = false;

#define LOG_TAG "AIS-catcher JNI"

#define LOGE(...) \
  __android_log_print(ANDROID_LO \
  G_ERROR, LOG_TAG, __VA_ARGS__)

#include "AIS-catcher.h"
#include "Receiver.h"
#include "JSONAIS.h"
#include "Signals.h"
#include "Common.h"
#include "Model.h"
#include "Network.h"
#include "WebViewer.h"
#include "Logger.h"

#include "RTLSDR.h"
#include "AIRSPYHF.h"
#include "HACKRF.h"
#include "RTLTCP.h"
#include "SpyServer.h"
#include "AIRSPY.h"

static int javaVersion;

static JavaVM *javaVm = nullptr;
static jclass javaClass = nullptr;
static jclass javaStatisticsClass = nullptr;

struct Statistics {
    uint64_t DataSize;
    int Total;
    int ChA;
    int ChB;
    int Msg[28];
    int Error;

} statistics;

WebViewer server;
static std::unique_ptr<WebViewer> webviewer = nullptr;
static std::unique_ptr<IO::TCPlistenerStreamer> TCP_listener = nullptr;
int webviewer_port = -1;

std::string nmea_msg;
std::string json_queue;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *, void *) {
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

std::string toString(JNIEnv* env, jstring jStr) {

    if (!jStr) {
        return std::string();
    }

    const char* chars = env->GetStringUTFChars(jStr, nullptr);
    if (!chars) return std::string();

    std::string result(chars);
    env->ReleaseStringUTFChars(jStr, chars);
    return result;
}

void pushStatistics(JNIEnv *env) {

    const int GB = 1000000000;
    env->SetStaticIntField(javaStatisticsClass,
                           env->GetStaticFieldID(javaStatisticsClass, "DataB", "I"),
                           (int)(statistics.DataSize % GB));
    env->SetStaticIntField(javaStatisticsClass,
                           env->GetStaticFieldID(javaStatisticsClass, "DataGB", "I"),
                           (int)(statistics.DataSize / GB));
    env->SetStaticIntField(javaStatisticsClass,
                           env->GetStaticFieldID(javaStatisticsClass, "Total", "I"),
                           statistics.Total);
    env->SetStaticIntField(javaStatisticsClass,
                           env->GetStaticFieldID(javaStatisticsClass, "ChA", "I"), statistics.ChA);
    env->SetStaticIntField(javaStatisticsClass,
                           env->GetStaticFieldID(javaStatisticsClass, "ChB", "I"), statistics.ChB);
    env->SetStaticIntField(javaStatisticsClass,
                           env->GetStaticFieldID(javaStatisticsClass, "Msg123", "I"),
                           statistics.Msg[1] + statistics.Msg[2] + statistics.Msg[3]);
    env->SetStaticIntField(javaStatisticsClass,
                           env->GetStaticFieldID(javaStatisticsClass, "Msg5", "I"),
                           statistics.Msg[5]);
    env->SetStaticIntField(javaStatisticsClass,
                           env->GetStaticFieldID(javaStatisticsClass, "Msg1819", "I"),
                           statistics.Msg[18] + statistics.Msg[19]);
    env->SetStaticIntField(javaStatisticsClass,
                           env->GetStaticFieldID(javaStatisticsClass, "Msg24", "I"),
                           statistics.Msg[24] + statistics.Msg[25]);
    env->SetStaticIntField(javaStatisticsClass,
                           env->GetStaticFieldID(javaStatisticsClass, "MsgOther", "I"),
                           statistics.Total -
                           (statistics.Msg[1] + statistics.Msg[2] + statistics.Msg[3] +
                            statistics.Msg[5] + statistics.Msg[18] + statistics.Msg[19] +
                            statistics.Msg[24] + statistics.Msg[25]));
}

static void callbackNMEA(JNIEnv *env, const std::string &str) {

    jstring jstr = env->NewStringUTF(str.c_str());
    jmethodID method = env->GetStaticMethodID(javaClass, "onNMEA", "(Ljava/lang/String;)V");
    env->CallStaticVoidMethod(javaClass, method, jstr);
}

static void callbackMessage(JNIEnv *env, const std::string &str) {

    jstring jstr = env->NewStringUTF(str.c_str());
    jmethodID method = env->GetStaticMethodID(javaClass, "onMessage", "(Ljava/lang/String;)V");
    env->CallStaticVoidMethod(javaClass, method, jstr);
}

static void callbackConsole(JNIEnv *env, const std::string &str) {

    jstring jstr = env->NewStringUTF(str.c_str());
    jmethodID method = env->GetStaticMethodID(javaClass, "onStatus", "(Ljava/lang/String;)V");
    env->CallStaticVoidMethod(javaClass, method, jstr);
}

static void callbackConsoleFormat(JNIEnv *env, const char *format, ...) {

    char buffer[256];
    va_list args;
    va_start (args, format);
    vsnprintf(buffer, 255, format, args);

    jstring jstr = env->NewStringUTF(buffer);
    jmethodID method = env->GetStaticMethodID(javaClass, "onStatus", "(Ljava/lang/String;)V");
    env->CallStaticVoidMethod(javaClass, method, jstr);

    va_end (args);
}

static void callbackUpdate(JNIEnv *env) {

    pushStatistics(env);

    jmethodID method = env->GetStaticMethodID(javaClass, "onUpdate", "()V");
    env->CallStaticVoidMethod(javaClass, method);
}

static void callbackError(JNIEnv *env, const std::string &str) {

    callbackConsole(env, str+"\r\n");
    Error() << str;

    jstring jstr = env->NewStringUTF(str.c_str());
    jmethodID method = env->GetStaticMethodID(javaClass, "onError", "(Ljava/lang/String;)V");
    env->CallStaticVoidMethod(javaClass, method, jstr);
}

// AIS-catcher model

class NMEAcounter : public StreamIn<AIS::Message> {
    std::string list;
    bool clean = true;

public:

    void Receive(const AIS::Message *data, int len, TAG &tag) {
        std::string str;

        for (int i = 0; i < len; i++) {
            for (const auto &s: data[i].NMEA) {
                str.append("\n" + s);
            }

            statistics.Total++;

            if (data[i].getChannel() == 'A')
                statistics.ChA++;
            else
                statistics.ChB++;

            int msg = data[i].type();

            if (msg > 27 || msg < 1) statistics.Error++;
            if (msg <= 27) statistics.Msg[msg]++;

            nmea_msg += str;
        }
    }
};

// Counting Data received from device
class RAWcounter : public StreamIn<RAW> {
public:

    void Receive(const RAW *data, int len, TAG &tag) {
        statistics.DataSize += data->size;
    }
};

struct Drivers {
    Device::RTLSDR RTLSDR;
    Device::RTLTCP RTLTCP;
    Device::SpyServer SPYSERVER;
    Device::AIRSPY AIRSPY;
    Device::AIRSPYHF AIRSPYHF;
} drivers;

std::vector<IO::UDPStreamer > UDP_connections;
std::vector<IO::TCPClientStreamer > TCP_connections;
std::vector<std::string> UDPhost;
std::vector<std::string> UDPport;
std::vector<bool> UDPJSON;
std::string TCP_listener_port;

bool sharing = false;
std::string sharingKey = "";

NMEAcounter NMEAcounter;
RAWcounter rawcounter;

Device::Device *device = nullptr;
static std::unique_ptr<AIS::Model> model = nullptr;
AIS::JSONAIS json2ais;

bool stop = false;

void StopRequest() {
    stop = true;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_InitNative(JNIEnv *env, jclass instance, jint port) {
    Logger::getInstance().setMaxBufferSize(50);

    env->GetJavaVM(&javaVm);
    javaVersion = env->GetVersion();
    javaClass = (jclass) env->NewGlobalRef(instance);

    Info() << "AIS-Catcher " VERSION;
    Info() << "Internal webserver running at port " << port;

    memset(&statistics, 0, sizeof(statistics));

    server.Set("PORT",std::to_string(port));
    server.Set("STATION","Android");
    server.Set("SHARE_LOC","ON");
    server.Set("REALTIME","ON");
    server.Set("LOG","ON");
    server.start();

    return 0;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_isStreaming(JNIEnv *, jclass) {
    if (device && device->isStreaming()) return JNI_TRUE;

    return JNI_FALSE;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_applySetting(JNIEnv *env, jclass, jstring dev, jstring setting, jstring param) {

    try {
        std::string d = toString(env,dev);
        std::string s = toString(env, setting);
        std::string p = toString(env, param);

        switch (d[0]) {
            case 't':
                Info() << "RTLTCP: " << s << " = " << p;
                drivers.RTLTCP.Set(s, p);
                break;
            case 'r':
                Info() << "RTLSDR: " << s << " = " << p;
                drivers.RTLSDR.Set(s, p);
                break;
            case 'm':
                Info() << "AIRSPY: " << s << " = " << p;
                drivers.AIRSPY.Set(s, p);
                break;
            case 'h':
                Info() << "AIRSPYHF: " << s << " = " << p;
                drivers.AIRSPYHF.Set(s, p);
                break;
            case 's':
                Info() << "SPYSERVER: " << s << " = " << p;
                drivers.SPYSERVER.Set(s, p);
                break;

        }

    } catch (std::exception& e) {
        callbackError(env, e.what());
        device = nullptr;
        return -1;
    }
    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_Run(JNIEnv *env, jclass) {


    const int TIME_INTERVAL = 1000;
    const int TIME_MAX = (TIME_CONSTRAINT * 1000) / TIME_INTERVAL;
    TAG tag;
    tag.mode = 7;

    try {
        Info() << "Creating UDP output channels";
        UDP_connections.resize(UDPhost.size());

        for (int i = 0; i < UDPhost.size(); i++) {
            UDP_connections[i].Set("host",UDPhost[i]).Set("port",UDPport[i]).Set("JSON",UDPJSON[i]?"on":"off");
            UDP_connections[i].Start();
            model->Output() >> UDP_connections[i];
        }

        if(!TCP_listener_port.empty()) {
            TCP_listener = std::make_unique<IO::TCPlistenerStreamer>();
            Info() << "Creating TCP listener at port " << TCP_listener_port;
            TCP_listener->Set("PORT", TCP_listener_port);
            TCP_listener->Set("TIMEOUT","0");
            TCP_listener->Set("JSON","false");
            model->Output() >> (*TCP_listener);
        }

        if(sharing) {
            Info() << "Creating Sharing output channel";
            int sharing_index = TCP_connections.size();
            TCP_connections.resize(sharing_index + 1);

            TCP_connections[sharing_index].Set("HOST", "aiscatcher.org").Set("PORT", "4242").Set("JSON", "on").Set("FILTER", "on").Set("GPS", "off");
            TCP_connections[sharing_index].Set("UUID", sharingKey);
            TCP_connections[sharing_index].Start();
            model->Output() >> TCP_connections[sharing_index];
        }

        if(webviewer) {
            Info() << "Starting Web Viewer";
            webviewer->start();
        }

        if(TCP_listener) {
            Info() << "Starting TCP listener";
            TCP_listener->Start();
        }
        Info() << "Start Device";
        device->setTag(tag);
        device->Play();

        stop = false;

        Info() << "Run Started";

        int time_idx = 0;

        while (device->isStreaming() && !stop) {
            std::this_thread::sleep_for(std::chrono::milliseconds(TIME_INTERVAL));

            callbackUpdate(env);
            if (!nmea_msg.empty()) {
                callbackNMEA(env, nmea_msg);
                nmea_msg = "";
            }
            if(++time_idx % 30 == 0)
            Info() << "Msg Count: " << statistics.Total;
        }

    }
    catch (std::exception& e) {
        callbackError(env, e.what());
    }

    try {
        device->Stop();

        model->Output().out.clear();

        for (auto &u: UDP_connections) u.Stop();
        for (auto &t: TCP_connections) t.Stop();

        if(TCP_listener) TCP_listener->Stop();

        UDP_connections.clear();
        TCP_connections.clear();

        UDPport.clear();
        UDPhost.clear();
        UDPJSON.clear();
        TCP_listener = nullptr;

        if(webviewer) {
            webviewer->close();
            webviewer.reset();
        }
        webviewer_port = -1;

    } catch (std::exception& e) {
        callbackError(env, e.what());
    }

    if (!stop) {
        callbackError(env, "Device disconnected");
    }

    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_Close(JNIEnv *env, jclass) {
    Info() << "Device closing";

    try {
        if (device) device->Close();
        device = nullptr;
        model.reset();
    }
    catch (std::exception& e) {
        callbackError(env, e.what());
        return -1;
    }
    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_forceStop(JNIEnv *env, jclass) {
    Info() << "Stop requested";
    stop = true;
    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_createReceiver(JNIEnv *env, jclass, jint source,
                                                             jint fd,  jint CGF_wide, jint model_type, jint FPDS) {

    Info() << "Creating Receiver (source = " << static_cast<int>(source)
              << ", fd = " << static_cast<int>(fd)
              << ", CGF wide = " << static_cast<int>(CGF_wide)
              << ", model = " << static_cast<int>(model_type)
              << ", FPDS = " << static_cast<int>(FPDS) << ")" << std::endl;
/*
    if (device != nullptr) {
        callbackConsole(env, "Error: device already assigned.");
        return -1;
    }
*/

    if (source == 0) {
        Info() << "Device: RTLTCP";
        device = &drivers.RTLTCP;
    } else if (source == 1) {
        Info() << "Device: RTLSDR";
        device = &drivers.RTLSDR;
    } else if (source == 2) {
        Info() << "Device: AIRSPY";
        device = &drivers.AIRSPY;
    } else if (source == 3) {
        Info() << "Device: AIRSPYHF";
        device = &drivers.AIRSPYHF;
    } else if (source == 4) {
        Info() << "Device: SPYSERVER";
        device = &drivers.SPYSERVER;
    } else {
        Info() << "Support for this device not included.";
        return -1;
    }

    try {
        device->out.clear();
        device->OpenWithFileDescriptor(fd);
        device->setFrequency(162000000);
    }
    catch (std::exception& e) {
        callbackError(env, e.what());
        device = nullptr;
        return -1;
    }

    Info() << "Creating Model";
    try {

        Info() << "Building model with sampling rate: " << device->getSampleRate() / 1000;

        model.reset();

        if(device && device->getFormat() == Format::TXT) {
            Info() << "Model: NMEA";

            model = std::make_unique<AIS::ModelNMEA>();
            model->buildModel('A','B',device->getSampleRate(), false, device);
        }
        else {
            if(model_type == 0) {

                Info() << "Model: default";
                model = std::make_unique<AIS::ModelDefault>();

                std::string s = (CGF_wide == 0)?"OFF":"ON";
                model->Set("AFC_WIDE",s);
                Info() << "AFC Wide " << s;
            }
            else {
                Info() << "Model: Base (FM)";
                model = std::make_unique<AIS::ModelBase>();
            }

            std::string s = (FPDS == 0)?"OFF":"ON";
            model->Set("FP_DS",s);
            Info() <<  "Fixed Point Downsampler: " << s;

            model->buildModel('A','B',device->getSampleRate(), false, device);
        }

    } catch (std::exception& e) {
        callbackError(env, e.what());
        device = nullptr;
        return -1;
    }

    device->out >> rawcounter;
    model->Output() >> NMEAcounter;
    json2ais.out.clear();
    model->Output() >> json2ais;
    server.connect(*model, json2ais.out, *device);

    Info() << "Creating additional Web Viewer";

    if(webviewer) webviewer.reset();

    if(webviewer_port != -1) {
        webviewer = std::make_unique<WebViewer>();

        if(!webviewer) {
            Critical() << "Cannot create Web Viewer";
            throw std::runtime_error("Cannot create Web Viewer)");
        }

        webviewer->Set("PORT", std::to_string(webviewer_port));
        webviewer->Set("STATION", "Android");
        webviewer->Set("SHARE_LOC","ON");
        webviewer->Set("REALTIME","ON");
    }

    if(webviewer && webviewer_port != -1)
        webviewer->connect(*model, json2ais.out, *device);

    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_createUDP(JNIEnv *env, jclass clazz, jstring h,
                                                        jstring p, jboolean J) {
    try {
        UDPport.resize(UDPport.size() + 1);
        UDPhost.resize(UDPhost.size() + 1);
        UDPJSON.resize(UDPJSON.size() + 1);

        jboolean b;
        std::string host = toString(env,h); //(env)->GetStringUTFChars(h, &b);
        std::string port = toString(env, p); //(env)->GetStringUTFChars(p, &b);
        bool JSON = J;

        UDPport[UDPport.size() - 1] = port;
        UDPhost[UDPhost.size() - 1] = host;
        UDPJSON[UDPJSON.size()-1] = JSON;

        Info() << "UDP: " << host << ":" << port << (J ? std::string(" (JSON)") : std::string(" (NMEA)"));
    } catch (std::exception& e) {
        callbackError(env, e.what());
        device = nullptr;
        return -1;
    }
    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_createWebViewer(JNIEnv *env, jclass clazz,
                                                              jstring p) {
    try {
        jboolean b;
        std::string port = toString(env,p); //(env)->GetStringUTFChars(p, &b);
        webviewer_port = std::stoi(port);

        Info() << "Web Viewer active on port " << port;

    } catch (std::exception& e) {
        callbackError(env, e.what());
        device = nullptr;
        return -1;
    }
    return 0;}

extern "C"
JNIEXPORT jint JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_createSharing(JNIEnv *env, jclass clazz, jboolean b,
                                                            jstring k) {
    if(b) {
        jboolean isCopy;
        std::string key = toString(env,k); //(env)->GetStringUTFChars(k, &isCopy);

        sharing = communityFeed = true;
        sharingKey = key;
        Info() << "Community Sharing: " << key;
    }
    else {
        sharing = communityFeed = false;
        sharingKey = "";
    }
    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_getSampleRate(JNIEnv *env, jclass clazz) {
    if (device == NULL) return 0;

    return device->getSampleRate();
}


extern "C"
JNIEXPORT void JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_00024Statistics_Init(JNIEnv *env, jclass instance) {
    javaStatisticsClass = (jclass) env->NewGlobalRef(instance);
    memset(&statistics, 0, sizeof(statistics));
}

extern "C"
JNIEXPORT void JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_00024Statistics_Reset(JNIEnv *env, jclass instance) {

    memset(&statistics, 0, sizeof(statistics));

    server.Reset();

    callbackUpdate(env);
    callbackNMEA(env, "");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_setLatLon(JNIEnv *env, jclass clazz, jfloat lat,
                                                        jfloat lon) {
    server.Set("LAT",std::to_string(lat));
    server.Set("LON",std::to_string(lon));

    if(webviewer) {
        webviewer->Set("LAT",std::to_string(lat));
        webviewer->Set("LON",std::to_string(lon));
    }
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_getLibraryVersion(JNIEnv *env, jobject thiz) {
    std::string message = VERSION_DESCRIBE;
    return env->NewStringUTF(message.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_setDeviceDescription(JNIEnv *env, jclass clazz,
                                                                   jstring p, jstring v,
                                                                   jstring s) {

    std::string product = toString(env,p);
    std::string vendor = toString(env,v);
    std::string serial = toString(env, s);

    server.setDeviceDescription(product.c_str(), vendor.c_str(), serial.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_getRateDescription(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(device->getRateDescription().c_str());
}


extern "C"
JNIEXPORT jint JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_createTCPlistener(JNIEnv *env, jclass clazz,
                                                                jstring p) {
    std::string port = toString(env, p);
    TCP_listener_port = port;
    Info() << "TCP Listener: "  << port ;
    return 0;
}