
# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)

# Declares and names the project.

project("AIScatcherNDK")

set(OPTIMIZATION_FLAGS "-Ofast")
#set(OPTIMIZATION_FLAGS "-g")

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

        ./AIS-catcher/Application/Receiver.cpp ./AIS-catcher/Application/Config.cpp ./AIS-catcher/Ships/DB.cpp ./AIS-catcher/DBMS/PostgreSQL.cpp
        ./AIS-catcher/Device/AIRSPYHF.cpp ./AIS-catcher/Device/FileWAV.cpp ./AIS-catcher/Device/RTLSDR.cpp ./AIS-catcher/Device/SDRPLAY.cpp ./AIS-catcher/DSP/Demod.cpp ./AIS-catcher/DSP/Model.cpp
        ./AIS-catcher/Library/AIS.cpp ./AIS-catcher/Library/JSONAIS.cpp ./AIS-catcher/Library/Keys.cpp ./AIS-catcher/IO/HTTPClient.cpp
        ./AIS-catcher/Device/FileRAW.cpp ./AIS-catcher/Device/HACKRF.cpp ./AIS-catcher/Device/UDP.cpp ./AIS-catcher/Device/RTLTCP.cpp
        ./AIS-catcher/Device/ZMQ.cpp ./AIS-catcher/Device/SoapySDR.cpp ./AIS-catcher/Device/SpyServer.cpp ./AIS-catcher/Library/Message.cpp ./AIS-catcher/Library/NMEA.cpp
        ./AIS-catcher/Library/Utilities.cpp ./AIS-catcher/Library/TCP.cpp ./AIS-catcher/JSON/JSON.cpp ./AIS-catcher/IO/Network.cpp ./AIS-catcher/IO/HTTPServer.cpp
        ./AIS-catcher/JSON/StringBuilder.cpp ./AIS-catcher/JSON/Parser.cpp ./AIS-catcher/Device/AIRSPY.cpp ./AIS-catcher/Device/Serial.cpp
        ./AIS-catcher/DSP/DSP.cpp  ./AIS-catcher/IO/IO.cpp

        JNI/AIScatcherNDK.cpp)

include_directories(
        ./libusb/android ./libusb/libusb ./libusb/libusb/os
        ./rtl-sdr/include
        ./airspyone_host/libairspy/src
        ./airspyhf/libairspyhf/src
        ./AIS-catcher ./AIS-catcher/Application ./AIS-catcher/IO ./AIS-catcher/Library ./AIS-catcher/Ships ./AIS-catcher/DBMS ./AIS-catcher/DSP ./AIS-catcher/Device)

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
