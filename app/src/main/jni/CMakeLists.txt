
# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)

# Declares and names the project.

project("AIScatcherNDK")

set(OPTIMIZATION_FLAGS "-Ofast")
#set(CMAKE_CXX_FLAGS "${OPTIMIZATION_FLAGS}")
#set(CMAKE_C_FLAGS "${OPTIMIZATION_FLAGS}")

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

        ./AIS-Catcher/Device/AIRSPYHF.cpp ./AIS-Catcher/Device/FileWAV.cpp ./AIS-Catcher/Device/RTLSDR.cpp ./AIS-Catcher/Device/SDRPLAY.cpp
        ./AIS-Catcher/DSP/Demod.cpp ./AIS-Catcher/DSP/Model.cpp ./AIS-Catcher/Library/AIS.cpp ./AIS-Catcher/Device/FileRAW.cpp
        ./AIS-Catcher/Device/HACKRF.cpp ./AIS-Catcher/Device/RTLTCP.cpp ./AIS-Catcher/Device/ZMQ.cpp ./AIS-Catcher/Library/Utilities.cpp
        ./AIS-Catcher/Device/AIRSPY.cpp ./AIS-Catcher/DSP/DSP.cpp ./AIS-Catcher/IO/IO.cpp

        JNI/AIScatcherNDK.cpp)

include_directories(
        ./libusb/android ./libusb/libusb ./libusb/libusb/os
        ./rtl-sdr/include
        ./airspyone_host/libairspy/src
        ./airspyhf/libairspyhf/src
        ./AIS-Catcher ./AIS-Catcher/Application ./AIS-Catcher/IO ./AIS-Catcher/Library ./AIS-Catcher/DSP ./AIS-Catcher/Device)

add_definitions(-DHASRTLSDR -DHASRTL_ANDROID -DHASAIRSPY -DHASAIRSPY_ANDROID -D HASAIRSPYHF -DHASAIRSPYHF_ANDROID)

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
