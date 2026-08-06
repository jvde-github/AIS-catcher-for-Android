
cmake_minimum_required(VERSION 3.10.2)

project("AIScatcherNDK")

set(OPTIMIZATION_FLAGS "-O3")

set(CMAKE_CXX_FLAGS "${OPTIMIZATION_FLAGS}")
set(CMAKE_C_FLAGS "${OPTIMIZATION_FLAGS}")

add_link_options("-Wl,--build-id=none" "-Wl,--strip-debug" "-Wl,--hash-style=gnu")
add_compile_options(-O3 -ffast-math -Wno-builtin-macro-redefined)

set(BUILD_DATE "NA")
find_package(Git QUIET)
if(GIT_FOUND)
    execute_process(
        COMMAND ${GIT_EXECUTABLE} log -1 --format=%cd "--date=format:%b %e %Y"
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        OUTPUT_VARIABLE GIT_BUILD_DATE
        OUTPUT_STRIP_TRAILING_WHITESPACE
        ERROR_QUIET)
    if(GIT_BUILD_DATE)
        set(BUILD_DATE "${GIT_BUILD_DATE}")
    endif()
endif()
add_compile_definitions(__DATE__="${BUILD_DATE}")

add_library(
        AIScatcherNDK

        SHARED

        ./libusb/libusb/core.c
        ./libusb/libusb/descriptor.c
        ./libusb/libusb/hotplug.c
        ./libusb/libusb/io.c
        ./libusb/libusb/sync.c
        ./libusb/libusb/strerror.c
        ./libusb/libusb/os/linux_usbfs.c
        ./libusb/libusb/os/events_posix.c
        ./libusb/libusb/os/threads_posix.c
        ./libusb/libusb/os/linux_netlink.c

        ./rtl-sdr/src/librtlsdr.c
        ./rtl-sdr/src/tuner_e4k.c ./rtl-sdr/src/tuner_fc0012.c ./rtl-sdr/src/tuner_fc0013.c ./rtl-sdr/src/tuner_fc2580.c ./rtl-sdr/src/tuner_r82xx.c

        ./airspyone_host/libairspy/src/airspy.c ./airspyone_host/libairspy/src/iqconverter_float.c
        ./airspyone_host/libairspy/src/iqconverter_int16.c

        ./airspyhf/libairspyhf/src/airspyhf.c
        ./airspyhf/libairspyhf/src/iqbalancer.c

        ./AIS-catcher/Source/Application/Receiver.cpp ./AIS-catcher/Source/Web/WebDB.cpp ./AIS-catcher/Source/Application/Config.cpp ./AIS-catcher/Source/Tracking/DB.cpp
        ./AIS-catcher/Source/Tracking/Ships.cpp  ./AIS-catcher/Source/DBMS/PostgreSQL.cpp
        ./AIS-catcher/Source/Device/AIRSPYHF.cpp ./AIS-catcher/Source/Device/FileWAV.cpp ./AIS-catcher/Source/Device/RTLSDR.cpp ./AIS-catcher/Source/Device/SDRPLAY.cpp
        ./AIS-catcher/Source/DSP/Demod.cpp ./AIS-catcher/Source/DSP/Model.cpp  ./AIS-catcher/Source/Marine/AIS.cpp ./AIS-catcher/Source/JSON/JSONAIS.cpp
        ./AIS-catcher/Source/JSON/Keys.cpp ./AIS-catcher/Source/IO/HTTPClient.cpp
        ./AIS-catcher/Source/Device/FileRAW.cpp ./AIS-catcher/Source/Device/HACKRF.cpp ./AIS-catcher/Source/Device/UDP.cpp ./AIS-catcher/Source/Device/RTLTCP.cpp
        ./AIS-catcher/Source/Device/ZMQ.cpp ./AIS-catcher/Source/Device/SoapySDR.cpp ./AIS-catcher/Source/Device/SpyServer.cpp ./AIS-catcher/Source/Marine/Message.cpp
        ./AIS-catcher/Source/Marine/NMEA.cpp
        ./AIS-catcher/Source/Utilities/Convert.cpp ./AIS-catcher/Source/Utilities/Parse.cpp ./AIS-catcher/Source/Utilities/Helper.cpp
        ./AIS-catcher/Source/JSON/JSON.cpp ./AIS-catcher/Source/IO/Network.cpp
        ./AIS-catcher/Source/IO/HTTPServer.cpp ./AIS-catcher/Source/IO/TCPServer.cpp
        ./AIS-catcher/Source/JSON/Parser.cpp ./AIS-catcher/Source/Device/AIRSPY.cpp ./AIS-catcher/Source/Device/Serial.cpp
        ./AIS-catcher/Source/Device/HYDRASDR.cpp
        ./AIS-catcher/Source/Aviation/ADSB.cpp
        ./AIS-catcher/Source/DSP/DSP.cpp ./AIS-catcher/Source/Web/WebViewer.cpp ./AIS-catcher/Source/Web/Prometheus.cpp ./AIS-catcher/Source/IO/Protocol.cpp
        ./AIS-catcher/Source/Library/Logger.cpp ./AIS-catcher/Source/Application/DeviceManager.cpp ./AIS-catcher/Source/Utilities/StreamHelpers.cpp
        ./AIS-catcher/Source/Aviation/Basestation.cpp ./AIS-catcher/Source/Aviation/Beast.cpp  ./AIS-catcher/Source/Utilities/TemplateString.cpp ./AIS-catcher/Source/Web/MapTiles.cpp
        ./AIS-catcher/Source/Web/BackupManager.cpp ./AIS-catcher/Source/Web/FrontendConfig.cpp
        ./AIS-catcher/Source/Tracking/ReceiverTracker.cpp ./AIS-catcher/Source/DSP/Decoder/V2/V2Engine.cpp

        JNI/AIScatcherNDK.cpp)

include_directories(
        # Order matters: Filters.h, airspy.h, airspyhf.h and config.h collide case-insensitively
        ./AIS-catcher/Source/DSP
        ./libusb/android ./libusb/libusb ./libusb/libusb/os
        ./rtl-sdr/include
        ./airspyone_host/libairspy/src
        ./airspyhf/libairspyhf/src
        ./AIS-catcher/Source ./AIS-catcher/Source/Application ./AIS-catcher/Source/Web ./AIS-catcher/Source/IO ./AIS-catcher/Source/Library ./AIS-catcher/Source/Tracking ./AIS-catcher/Source/JSON
        ./AIS-catcher/Source/DBMS ./AIS-catcher/Source/DSP ./AIS-catcher/Source/Device ./AIS-catcher/Source/Utilities ./AIS-catcher/Source/Aviation ./AIS-catcher/Source/Marine)

add_definitions(-DHASRTLSDR -DHASRTLSDR_BIASTEE -DHASRTL_ANDROID -DHASAIRSPY -DHASAIRSPY_ANDROID -D HASAIRSPYHF -DHASAIRSPYHF_ANDROID -DHASRTLSDR_TUNERBW)

find_package(openssl REQUIRED CONFIG)
add_definitions(-DHASOPENSSL)

add_definitions(-DHASZLIB)

find_library(
        log-lib

        log)

target_link_libraries(
        AIScatcherNDK

        ${log-lib}

        openssl::ssl
        openssl::crypto

        z)
