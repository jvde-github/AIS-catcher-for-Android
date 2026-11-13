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
#include <deque>
#include <string>
#include <sstream>
#include <atomic>

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
#include "Site.h"
#include "Logger.h"

// by their directory: AIRSPY.h and AIRSPYHF.h are the libraries' own headers on a case-insensitive disk
#include "Device/RTLSDR.h"
#include "Device/AIRSPYHF.h"
#include "Device/HACKRF.h"
#include "Device/RTLTCP.h"
#include "Device/SpyServer.h"
#include "Device/AIRSPY.h"

#include "Keys.h"

static AIS::Keys deviceSettingKey(const std::string &s) {
    if (s == "RATE")       return AIS::KEY_SETTING_SAMPLE_RATE;
    if (s == "BW")         return AIS::KEY_SETTING_BANDWIDTH;
    if (s == "TUNER")      return AIS::KEY_SETTING_TUNER;
    if (s == "FREQOFFSET") return AIS::KEY_SETTING_FREQOFFSET;
    if (s == "RTLAGC")     return AIS::KEY_SETTING_RTLAGC;
    if (s == "BIASTEE")    return AIS::KEY_SETTING_BIASTEE;
    if (s == "HOST")       return AIS::KEY_SETTING_HOST;
    if (s == "PORT")       return AIS::KEY_SETTING_PORT;
    if (s == "PROTOCOL")   return AIS::KEY_SETTING_PROTOCOL;
    if (s == "GAIN")       return AIS::KEY_SETTING_GAIN;
    if (s == "LINEARITY")  return AIS::KEY_SETTING_LINEARITY;
    return (AIS::Keys)-1;
}

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

// What lives as long as the app: the phone as the one station, the tracker
// and the viewers, the app's own on its port and one on the network when the
// settings ask for it
static Site site;
static int viewer_port = -1;
int webviewer_port = -1;
static float station_lat = LAT_UNDEFINED, station_lon = LON_UNDEFINED;
static std::string device_product, device_vendor, device_serial;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *, void *) {
    return JNI_VERSION_1_6;
}

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

class NMEAcounter : public StreamIn<AIS::Message> {
    std::string list;
    bool clean = true;

public:

    void Receive(const AIS::Message *data, int len, TAG &tag) {
        for (int i = 0; i < len; i++) {
            statistics.Total++;

            if (data[i].getChannel() == 'A')
                statistics.ChA++;
            else
                statistics.ChB++;

            int msg = data[i].type();

            if (msg > 27 || msg < 1) statistics.Error++;
            if (msg <= 27) statistics.Msg[msg]++;
        }
    }
};

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

std::vector<std::string> UDPhost;
std::vector<std::string> UDPport;
std::vector<bool> UDPJSON;
std::string TCP_listener_port;

bool sharing = false;
std::string sharingKey = "";

int own_mmsi = 0;
int own_mode = 0;
int own_interval = 0;

bool http_enabled = false;
bool http_gzip = false;
std::string http_url, http_userpwd, http_id, http_interval, http_protocol;

NMEAcounter NMEAcounter;
RAWcounter rawcounter;

Device::Device *device = nullptr;
static std::unique_ptr<AIS::Model> model = nullptr;
AIS::JSONAIS json2ais;

// The app's one device and model as the site reaches them, to put its consumers on
struct AppWiring : EngineWiring {
    void unwire() override {
        model->Output().out.clear();
        json2ais.out.clear();
        model->Output() >> NMEAcounter;
        model->Output() >> json2ais;
    }
    void feed(Tracker &t, const TrackerSpec *) override {
        json2ais.out.Connect(&t.json());
    }
    uint64_t route(const std::vector<std::string> &, uint64_t groups_in, const std::string &) override {
        return groups_in;
    }
    void wire(IO::OutputMessage &o) override {
        const bool json = o.fmt == MessageFormat::JSON_FULL || o.fmt == MessageFormat::JSON_ANNOTATED ||
                          o.fmt == MessageFormat::JSON_SPARSE;
        if (json)
            json2ais.out.Connect((StreamIn<JSON::JSON> *) &o);
        else
            model->Output().out.Connect((StreamIn<AIS::Message> *) &o);
    }
    uint64_t radios() override { return GROUPS_ALL; }
} wiring;

// the phone, where its GPS puts it, on the map; riding the own vessel when there is one
static void publishStation() {
    StationRegistry stations;
    uint64_t seq = 0;
    StationRegistry::Station s;
    s.id = 1;
    s.name = "Android";
    s.lat = station_lat;
    s.lon = station_lon;
    s.mmsi = own_mmsi > 0 ? (uint32_t)own_mmsi : 0;
    s.gps = false;
    s.share_loc = true;
    stations.define(s, seq);
    site.publishStations(stations);
}

// the viewers: the app's own, for this phone only, and the one on the network
static void configureSite() {
    SiteConfig cfg;
    ServerSpec own;
    own.port = viewer_port;
    own.viewer.station = "Android";
    own.viewer.realtime = true;
    own.viewer.log = true;
    cfg.servers.push_back(own);
    if (webviewer_port != -1) {
        ServerSpec lan;
        lan.port = webviewer_port;
        lan.bind = "0.0.0.0";
        lan.viewer.station = "Android";
        lan.viewer.realtime = true;
        cfg.servers.push_back(lan);
    }
    site.configure(cfg);
}

std::atomic<bool> stop{false};

void StopRequest(bool) {
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

    viewer_port = port;
    configureSite();
    publishStation();

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

        AIS::Keys key = deviceSettingKey(s);
        if (key == (AIS::Keys)-1) {
            Error() << "Unknown device setting '" << s << "'";
            return -1;
        }

        switch (d[0]) {
            case 't':
                Info() << "RTLTCP: " << s << " = " << p;
                drivers.RTLTCP.SetKey(key, p);
                break;
            case 'r':
                Info() << "RTLSDR: " << s << " = " << p;
                drivers.RTLSDR.SetKey(key, p);
                break;
            case 'm':
                Info() << "AIRSPY: " << s << " = " << p;
                drivers.AIRSPY.SetKey(key, p);
                break;
            case 'h':
                Info() << "AIRSPYHF: " << s << " = " << p;
                drivers.AIRSPYHF.SetKey(key, p);
                break;
            case 's':
                Info() << "SPYSERVER: " << s << " = " << p;
                drivers.SPYSERVER.SetKey(key, p);
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
    tag.station_id = 1; // the phone's station, so distances are measured from it

    auto setOwnShipKeys = [](Setting &s) {
        if (own_mmsi <= 0) return;
        if (own_mode == 1)
            s.SetKey(AIS::KEY_SETTING_OWN_INTERVAL, std::to_string(own_interval));
        else if (own_mode == 2) {
            s.SetKey(AIS::KEY_SETTING_FILTER, "on");
            s.SetKey(AIS::KEY_SETTING_BLOCK_MMSI, std::to_string(own_mmsi));
        }
    };

    try {
        // the outputs: the site starts them, puts them on the model and stops them with the run
        Outputs outs;
        auto add = [&](const std::shared_ptr<IO::OutputMessage> &o, const std::string &told) {
            setOwnShipKeys(*o);
            outs.list.push_back(o);
            outs.told.push_back(told);
        };

        Info() << "Creating UDP output channels";

        for (int i = 0; i < UDPhost.size(); i++) {
            auto conn = std::make_shared<IO::UDPStreamer>();
            conn->SetKey(AIS::KEY_SETTING_HOST,UDPhost[i]).SetKey(AIS::KEY_SETTING_PORT,UDPport[i]).SetKey(AIS::KEY_SETTING_MSGFORMAT,UDPJSON[i]?"JSON_NMEA":"NMEA");
            add(conn, "udp " + UDPhost[i] + ":" + UDPport[i]);
        }

        if(!TCP_listener_port.empty()) {
            auto listener = std::make_shared<IO::TCPlistenerStreamer>();
            Info() << "Creating TCP listener at port " << TCP_listener_port;
            listener->SetKey(AIS::KEY_SETTING_PORT, TCP_listener_port);
            listener->SetKey(AIS::KEY_SETTING_TIMEOUT,"0");
            listener->SetKey(AIS::KEY_SETTING_MSGFORMAT,"NMEA");
            add(listener, "tcp listener " + TCP_listener_port);
        }

        if(sharing) {
            Info() << "Creating Sharing output channel";
            auto conn = std::make_shared<IO::TCPClientStreamer>();

            conn->SetKey(AIS::KEY_SETTING_HOST, "aiscatcher.org").SetKey(AIS::KEY_SETTING_PORT, "4242").SetKey(AIS::KEY_SETTING_MSGFORMAT, "COMMUNITY_HUB").SetKey(AIS::KEY_SETTING_FILTER, "on").SetKey(AIS::KEY_SETTING_GPS, "off");
            conn->SetKey(AIS::KEY_SETTING_UUID, sharingKey);
            add(conn, "sharing");
            outs.feed = conn.get();
            outs.sharing_defined = true;
        }

        if(http_enabled) {
            Info() << "Creating HTTP output to " << http_url;
            auto http = std::make_shared<IO::HTTPStreamer>();
            http->SetKey(AIS::KEY_SETTING_URL, http_url);
            if(!http_userpwd.empty()) http->SetKey(AIS::KEY_SETTING_USERPWD, http_userpwd);
            if(!http_id.empty())      http->SetKey(AIS::KEY_SETTING_STATIONID, http_id);
            if(!http_interval.empty())http->SetKey(AIS::KEY_SETTING_INTERVAL, http_interval);
            http->SetKey(AIS::KEY_SETTING_PROTOCOL, http_protocol);
            http->SetKey(AIS::KEY_SETTING_GZIP, http_gzip ? "on" : "off");
            add(http, "http " + http_url);
            outs.http = true;
        }

        // the device, for the viewer's statistics
        DeviceFacts d;
        const std::string serial = device_serial.empty() ? "-" : device_serial;
        d.product = device_product.empty() ? device->getProduct() : device_product;
        d.vendor = device_vendor.empty() ? "-" : device_vendor;
        d.serial = serial;
        d.sample_rate = device->getRateDescription();
        d.label = d.product + (serial != "-" ? " " + serial : "");
        d.model = model->getName();
        site.attachEngine(EngineFacts(1, d), wiring, std::move(outs));

        Info() << "Start Device";
        device->setTag(tag);
        device->Play();

        stop = false;

        Info() << "Run Started";

        int time_idx = 0;

        // the network outputs make and keep their connections from poll(), as the
        // engine's main loop does for the command-line program: every 50 ms, off the
        // decoding thread
        const int POLL_INTERVAL = 50;

        while (device->isStreaming() && !stop) {
            for (int i = 0; i < TIME_INTERVAL / POLL_INTERVAL && device->isStreaming() && !stop; i++) {
                std::this_thread::sleep_for(std::chrono::milliseconds(POLL_INTERVAL));
                site.pollOutputs();
            }

            callbackUpdate(env);
            if(++time_idx % 30 == 0)
            Info() << "Msg Count: " << statistics.Total;
        }

    }
    catch (std::exception& e) {
        callbackError(env, e.what());
    }

    try {
        if (device) device->Stop();

        model->Output().out.clear();
        json2ais.out.clear();

        // the outputs stop with the engine
        site.detachEngine();

        UDPport.clear();
        UDPhost.clear();
        UDPJSON.clear();

        // the network viewer goes with the run, as it came with it
        webviewer_port = -1;
        configureSite();

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
            if (own_mmsi > 0) {
                model->setOwnMMSI(own_mmsi);
                model->SetKey(AIS::KEY_SETTING_NMEA_REFRESH, "ON");
            }
            model->buildModel('A','B',device->getSampleRate(), false, device);
        }
        else {
            if(model_type == 0) {

                // AIS-catcher's default model; its frequency search is always wide
                Info() << "Model: default (AIS engine v2)";
                model = std::make_unique<AIS::ModelEngineV2>();
            }
            else {
                Info() << "Model: Base (FM)";
                model = std::make_unique<AIS::ModelBase>();
            }

            std::string s = (FPDS == 0)?"OFF":"ON";
            model->SetKey(AIS::KEY_SETTING_FP_DS,s);
            Info() <<  "Fixed Point Downsampler: " << s;

            if (own_mmsi > 0)
                model->setOwnMMSI(own_mmsi);
            model->buildModel('A','B',device->getSampleRate(), false, device);
        }

    } catch (std::exception& e) {
        callbackError(env, e.what());
        device = nullptr;
        return -1;
    }

    device->out >> rawcounter;
    *device >> site.received();

    // the network viewer, when the settings ask for one
    configureSite();

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
        std::string host = toString(env,h);
        std::string port = toString(env, p);
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
        std::string port = toString(env,p);
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
                                                            jstring k, jboolean anonymous) {
    if(b) {
        jboolean isCopy;
        std::string key = toString(env,k);

        sharing = communityFeed = true;
        sharingKey = key;

        if(anonymous)
            Warning() << "Sharing anonymously with the aiscatcher.org community. Choose Settings > Community Share to add your own sharing key or to stop sharing.";
        else
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
Java_com_jvdegithub_aiscatcher_AisCatcherJava_setOwnMMSI(JNIEnv *env, jclass clazz, jint mmsi,
                                                         jint mode, jint interval) {
    own_mmsi = mmsi;
    own_mode = mode;
    own_interval = interval;

    publishStation();

    if (own_mmsi > 0) {
        Info() << "Own MMSI: " << own_mmsi;
        if (own_mode == 1)
            Info() << "Own vessel: max 1 message per " << own_interval << "s on output feeds";
        else if (own_mode == 2)
            Info() << "Own vessel: blocked on output feeds";
    }
    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_createHTTP(JNIEnv *env, jclass clazz, jboolean b,
                                                         jstring url, jstring userpwd, jstring id,
                                                         jstring interval, jstring protocol, jboolean gzip) {
    if(b) {
        http_enabled = true;
        http_url = toString(env, url);
        http_userpwd = toString(env, userpwd);
        http_id = toString(env, id);
        http_interval = toString(env, interval);
        http_protocol = toString(env, protocol);
        http_gzip = gzip;
        Info() << "HTTP output: " << http_url << " (" << http_protocol << (http_gzip ? ", gzip" : "") << ")";
    }
    else {
        http_enabled = false;
        http_gzip = false;
        http_url = http_userpwd = http_id = http_interval = http_protocol = "";
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

    callbackUpdate(env);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_setLatLon(JNIEnv *env, jclass clazz, jfloat lat,
                                                        jfloat lon) {
    station_lat = lat;
    station_lon = lon;
    publishStation();
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

    device_product = product;
    device_vendor = vendor;
    device_serial = serial;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_jvdegithub_aiscatcher_AisCatcherJava_getRateDescription(JNIEnv *env, jclass clazz) {
    if (device == nullptr) return env->NewStringUTF("");
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