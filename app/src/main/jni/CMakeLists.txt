
# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)

# Declares and names the project.

project("AIScatcherNDK")

#set(OPTIMIZATION_FLAGS "-Ofast")
set(OPTIMIZATION_FLAGS "-g")

set(CMAKE_CXX_FLAGS "${OPTIMIZATION_FLAGS}")
set(CMAKE_C_FLAGS "${OPTIMIZATION_FLAGS}")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        AIScatcherNDK

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
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

        ./AIS-catcher/Source/Application/Receiver.cpp ./AIS-catcher/Source/Application/WebDB.cpp ./AIS-catcher/Source/Application/Config.cpp ./AIS-catcher/Source/Tracking/DB.cpp
        ./AIS-catcher/Source/Tracking/Ships.cpp  ./AIS-catcher/Source/DBMS/PostgreSQL.cpp
        ./AIS-catcher/Source/Device/AIRSPYHF.cpp ./AIS-catcher/Source/Device/FileWAV.cpp ./AIS-catcher/Source/Device/RTLSDR.cpp ./AIS-catcher/Source/Device/SDRPLAY.cpp
        ./AIS-catcher/Source/DSP/Demod.cpp ./AIS-catcher/Source/DSP/Model.cpp  ./AIS-catcher/Source/Marine/AIS.cpp ./AIS-catcher/Source/JSON/JSONAIS.cpp
        ./AIS-catcher/Source/JSON/Keys.cpp ./AIS-catcher/Source/IO/HTTPClient.cpp
        ./AIS-catcher/Source/Device/FileRAW.cpp ./AIS-catcher/Source/Device/HACKRF.cpp ./AIS-catcher/Source/Device/UDP.cpp ./AIS-catcher/Source/Device/RTLTCP.cpp
        ./AIS-catcher/Source/Device/ZMQ.cpp ./AIS-catcher/Source/Device/SoapySDR.cpp ./AIS-catcher/Source/Device/SpyServer.cpp ./AIS-catcher/Source/Marine/Message.cpp
        ./AIS-catcher/Source/Marine/NMEA.cpp
        ./AIS-catcher/Source/Utilities/Convert.cpp ./AIS-catcher/Source/Utilities/Serialize.cpp  ./AIS-catcher/Source/Utilities/Parse.cpp ./AIS-catcher/Source/Utilities/Helper.cpp
        ./AIS-catcher/Source/Library/TCP.cpp ./AIS-catcher/Source/JSON/JSON.cpp ./AIS-catcher/Source/IO/Network.cpp
        ./AIS-catcher/Source/IO/HTTPServer.cpp
        ./AIS-catcher/Source/JSON/StringBuilder.cpp ./AIS-catcher/Source/JSON/Parser.cpp ./AIS-catcher/Source/Device/AIRSPY.cpp ./AIS-catcher/Source/Device/Serial.cpp
        ./AIS-catcher/Source/Aviation/ADSB.cpp
        ./AIS-catcher/Source/DSP/DSP.cpp ./AIS-catcher/Source/Application/WebViewer.cpp ./AIS-catcher/Source/Application/Prometheus.cpp ./AIS-catcher/Source/IO/Protocol.cpp
        ./AIS-catcher/Source/Library/Logger.cpp ./AIS-catcher/Source/Application/DeviceManager.cpp ./AIS-catcher/Source/utilities/StreamHelpers.cpp
        ./AIS-catcher/Source/Aviation/Basestation.cpp ./AIS-catcher/Source/Aviation/Beast.cpp  ./AIS-catcher/Source/utilities/TemplateString.cpp ./AIS-catcher/Source/Application/MapTiles.cpp

        JNI/AIScatcherNDK.cpp)

include_directories(
        ./libusb/android ./libusb/libusb ./libusb/libusb/os
        ./rtl-sdr/include
        ./airspyone_host/libairspy/src
        ./airspyhf/libairspyhf/src
        ./AIS-catcher/Source ./AIS-catcher/Source/Application ./AIS-catcher/Source/IO ./AIS-catcher/Source/Library ./AIS-catcher/Source/Tracking ./AIS-catcher/Source/JSON
        ./AIS-catcher/Source/DBMS ./AIS-catcher/Source/DSP ./AIS-catcher/Source/Device ./AIS-catcher/Source/Utilities ./AIS-catcher/Source/Aviation ./AIS-catcher/Source/Marine)

add_definitions(-DHASRTLSDR -DHASRTLSDR_BIASTEE -DHASRTL_ANDROID -DHASAIRSPY -DHASAIRSPY_ANDROID -D HASAIRSPYHF -DHASAIRSPYHF_ANDROID -DHASRTLSDR_TUNERBW)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        AIScatcherNDK

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})
