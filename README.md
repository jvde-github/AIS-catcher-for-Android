# AIS-catcher for Android - A multi-platform AIS receiver 
This Android App helps to change your Android device into a dual channel AIS receiver that can be used to pick up AIS signals from nearby vessels, even if offline!
The App directly accesses a Software Defined Radio USB device, like a  RTL-SDR dongle or an AirSpy decvice. Received vessels are visualized on the built-in map or messages are sent via UDP to plotting Apps like [Boat Beacon](https://pocketmariner.com/mobile-apps/boatbeacon/) or [OpenCPN](https://play.google.com/store/apps/details?id=org.opencpn.opencpn_free). A lightweight AIS receiver system when travelling. AIS-catcher for Android has been tested on an Odroid running Android.

An impression of AIS-catcher on the beach on a Galaxy Note 20 in July 2023 (thanks and credit: Roger G7RUH)
<p align="center">
<img src="https://github.com/jvde-github/AIS-catcher-for-Android/assets/52420030/11dbab61-03aa-4f7c-a9f5-ccd8f6ee29b5" width=30% height=30%>
<img src="https://github.com/jvde-github/AIS-catcher-for-Android/assets/52420030/03c4da4d-dc26-4499-9adb-f36c0e4f5c5a" width=30% height=30%>
</p>


Here you can find a [link](https://github.com/jvde-github/AIS-catcher-for-Android/releases/download/Edge/app-release-signed.apk) to the APK file for latest Edge version or visit the [Google Play Store](https://play.google.com/store/apps/details?id=com.jvdegithub.aiscatcher&gl=NL) – or find AIS-catcher [at IzzyOnDroid](https://apt.izzysoft.de/packages/com.jvdegithub.aiscatcher). The engine and visualizations are based on [AIS-catcher](https://github.com/jvde-github/AIS-catcher).

> ***NOTE: The Google Play Store introduced new requirements for developers to publish their personal details like address which we dont want to adhere to. Hence the app will be no longer available in the Play Store from mid August. The APK can still be downloaded and installed from here, or from IzzyOnDroid.***

AIS-catcher had a recent overhaul. The instructions below still are relevant but the visualization of the results is now based on the same code as the AIS-catcher web interface. The instructions will be updated in due course.
<p align="center">
<img src="https://github.com/jvde-github/AIS-catcher-for-Android/assets/52420030/076920d4-e75a-409c-87ea-d286df8eb046" width=20% height=20%>  &nbsp;
<img src="https://github.com/jvde-github/AIS-catcher-for-Android/assets/52420030/06f73ba9-3e0d-411d-85ef-ecb6730023b4" width=20% height=20%>  &nbsp;
<img src="https://github.com/jvde-github/AIS-catcher-for-Android/assets/52420030/dd867256-9e7e-4822-a1e2-dc6115400513" width=20% height=20%>  &nbsp; <br>
<img src="https://github.com/jvde-github/AIS-catcher-for-Android/assets/52420030/f29801ec-a4d5-4e2d-8568-aae240868048" width=20% height=20%>  &nbsp;
<img src="https://github.com/jvde-github/AIS-catcher-for-Android/assets/52420030/34368bcf-499b-4497-bfb0-2645bfb9fc2a" width=20% height=20%>  &nbsp;
<img src="https://github.com/jvde-github/AIS-catcher-for-Android/assets/52420030/6e57de02-cd9a-4778-81fe-4df2242e8907" width=20% height=20%>  &nbsp;
</p>

The requirements to receive AIS signals are: a RTL-SDR dongle (or alternatively an AirSpy Mini/R2/HF+), a simple antenna, 
an Android device with USB connector and an OTG cable to connect the dongle with your Android device. 
AIS-catcher only receives and processes signals and then forwards the messages over UDP or visualizes them on the build-in map (internet connection required).
And one more thing, you need to be in a region where there are ships broadcasting AIS signals, e.g. near the water.

### What's New?

- GUI has now been aligned with latest AIS-catcher versions
- Auto Start option
- Option to provide a web viewer at a defined port

<p align="center">
<img src="https://raw.githubusercontent.com/jvde-github/AIS-catcher/media/media/equipment.jpg" width=80% height=80%></p>

### Purpose and Disclaimer

```AIS-catcher for Android```  is created for research and educational purposes under the GPL license.
This program comes with **ABSOLUTELY NO WARRANTY**; This is free software, and you are welcome to redistribute it
under certain conditions. For details see the [project page](https://github.com/jvde-github/AIS-catcher-for-Android).

It is a hobby project and not tested and designed for reliability and correctness. 
You can play with the software but it is the user's responsibility to use it prudently. So,  **DO NOT** rely upon this software in any way including for navigation 
and/or safety of life or property purposes.
There are variations in the legislation concerning radio reception in the different administrations around the world. 
It is your responsibility to determine whether or not your local administration permits the reception and handling of AIS messages from ships and you can have this App on your phone. 
It is specifically forbidden to use this software for any illegal purpose whatsoever. 
The software is intended for use **only in those regions** where such use **is permitted**.

## Installation and Download

You can download AIS-catcher-for-Android in the [Release section](https://github.com/jvde-github/AIS-catcher-for-Android/releases) in the form of an APK-file. There are various resources on how to install an APK file available on the [web](https://www.androidauthority.com/how-to-install-apks-31494/).

Some Android manufacturers prefer battery life over proper functionality of Apps which might be particular relevant for a SDR AIS receiver. You can find tips for various devices at [https://dontkillmyapp.com/](https://dontkillmyapp.com/).

For a video of a field test of an early version [see YouTube](https://www.youtube.com/shorts/1ArB7GL_yV8). Below we have included a Getting Started tutorial when running with a RTL-SDR dongle. The steps for the AirSpy and TCP connections are very similar. Please notice that your phone or tablet has to power the USB device and run the decoding algorithm and this will be a drain on your battery. When sending UDP NMEA lines over the network or decoding from TCP (SpyServer or RTL-TCP) this will require serious network traffic. Advice is to do this when connected via WiFi.
Finally, the computationally intensive nature of AIS decoding requires the phone to give the Application sufficient run time. On some phones Android might restrict this and some tuning of the phone
settings might be required.


## Tutorial: Getting Started

### 1. Getting around: the main screen
The main screen of AIS-catcher is as follows:
<p align="center">
<img src="https://github.com/jvde-github/AIS-catcher/blob/a84ea19fb6198a292390a5b7ce64d06f5070f8a8/media/main_screen%5B1%5D.jpg" width=40% height=40%>
</p>
The tabs section at the top provides access to the main statistics and information when AIS-catcher is running whereas the bottom navigation bar has the buttons to start/stop the receiver, clear the logs/statistics and select the source device you want to use for reception.

### 2. Configuring the connection with OpenCPN and/or BoatBeacon
AIS-catcher is a simple receiver that decodes messages and can send the messages from ships onward to specialized plotting apps via UDP.
In this step  we are going to set up the outward connections to BoatBeacon and OpenCPN. For this we will use port ``10110`` for BoatBeacon and ``10111`` for OpenCPN. Press the 3 vertical dots on the top right and select the **Setting** option. Scroll to the bottom and activate the two UDP output connections via the switch and set the parameters as follows:
<p align="center">
<img src="https://github.com/jvde-github/AIS-catcher/blob/d7786eb75a15bcf230bf5e3a7f842de32e05f2bf/media/settings_UDP%5B1%5D.jpg" width=40% height=40%>
</p>

### 3. Setting up the Connection on OpenCPN 
Next we start OpenCPN and click on **Options** (top left icon) and choose the **Connections** tab. We need to add a Network connection using UDP at address ``0.0.0.0`` dataport ``10111``. 
You could initially select ``Show NMEA Debug Window`` as extra option which will give you a window in OpenCPN that shows all incoming NMEA messages it receives from AIS-catcher. This could be helpful
debugging a connection issue between the receiver and OpenCPN. The **Connections** tab will look something like:
<p align="center">
<img src="https://github.com/jvde-github/AIS-catcher/blob/90d0cb0cedc1220321c474621cb13eb787d6087d/media/OpenCPN_UDP_settings%5B1%5D.jpg" width=40% height=40%>
</p>

### 4. Granting AIS-catcher access to the USB dongle
Next we connect AIS-catcher to the RTL-SDR dongle. By default the user needs to give applications the rights to use a USB device. 
For this connect the dongle with your Android device using the OTG cable (if needed) and, if all is well, you should be asked if AIS-catcher can get access. 
With Dutch language settings (sorry) this should look like:

<p align="center">
<img src="https://github.com/jvde-github/AIS-catcher/blob/eb25b78295fe2320683bf569c346a2157e8b8c69/media/grant_access_to_USB%5B1%5D.jpg" width=40% height=40%>
</p>

Accept the request and consider giving AIS-catcher permanent access to the device so this step can be skipped in the future.

### 5. Configuring the RTL-SDR dongle
Next go back to the **Settings** menu via the 3 vertical dots on the main screen and set up the RTL-SDR settings:

<p align="center">
<img src="https://github.com/jvde-github/AIS-catcher/blob/b9093aae8ee7545b6dd5f7629e546691c9bce6a8/media/setting_RTL-SDR%5B2%5D.jpg" width=40% height=40%>
</p>

These settings should be ok but don't forget to set the frequency correction in PPM if needed for your device. You can set the dongle settings at any point in time but they will only
become active when a new run is started. 

### 6. Selecting the input source
In the Main screen select the **Source** by clicking the right-most item in the bottom navigation bar. Select the RTL-SDR device:
<p align="center">
<img src="https://github.com/jvde-github/AIS-catcher/blob/b3a5d1f26a4452b8707b6d79408b6e6c39d30889/media/slect_device%5B1%5D.jpg" width=40% height=40%>
</p>

The bottom navigation bar should show which device is currently active and will be used for AIS reception.

### 7. Running AIS-catcher
In the main screen now click **Start** on the left in the bottom navigation bar. This starts the run and a notification is given that a foreground service is started. 
The navigation tabs allow you to see different statistics during the run (like message count (STAT), messages from the receiver (LOG) and received NMEA lines (NMEA) ).
<p align="center">
<img src="https://github.com/jvde-github/AIS-catcher/blob/1c2b33d6b7c9df0d5a85e3224a196481d7df92ba/media/running_main_screen%5B1%5D.jpg" width=40% height=40%>
<img src="https://github.com/jvde-github/AIS-catcher/blob/1c2b33d6b7c9df0d5a85e3224a196481d7df92ba/media/running_console%5B1%5D.jpg" width=40% height=40%>
<img src="https://github.com/jvde-github/AIS-catcher/blob/1c2b33d6b7c9df0d5a85e3224a196481d7df92ba/media/running_nmea_screen%5B1%5D.jpg" width=40% height=40%>
<img src="https://github.com/jvde-github/AIS-catcher/blob/1c2b33d6b7c9df0d5a85e3224a196481d7df92ba/media/running_OCPN%5B1%5D.jpg" width=40% height=40%>
</p>

AIS-catcher will run as a foreground service so the app will continue to receive messages when closed. That's all there is to it. Have fun!

## Credits

AIS-catcher for Android uses the following libraries:

**libusb-1.0.26+**: <a href="https://github.com/libusb/libusb">https://github.com/libusb/libusb</a>

libusb is a library for USB device access from Linux, macOS, Windows, OpenBSD/NetBSD, Haiku and Solaris userspace. It is written in C (Haiku backend in C++) and licensed under the GNU Lesser General Public License version 2.1 or, at your option, any later version (see COPYING).

**rtl-sdr**: <a href="https://github.com/osmocom/rtl-sdr">https://github.com/osmocom/rtl-sdr</a>

Turns your Realtek RTL2832 based DVB dongle into a SDR receiver. Licensed under the GPL-2.0 license.
Modified for Android to open devices with file descriptors: https://github.com/jvde-github/rtl-sdr.

**airspyhf**: <a href="https://github.com/airspy/airspyhf">https://github.com/airspy/airspyhf</a>

This repository contains host software (Linux/Windows) for Airspy HF+, a high performance software defined radio for the HF and VHF bands. Licensed under the BSD-3-Clause license. Modified for Android to open devices with file descriptors: https://github.com/jvde-github/airspyhf.

**airspyone_host**: <a href="https://github.com/airspy/airspyone_host">https://github.com/airspy/airspyone_host</a>

AirSpy usemode driver and associated tools. Modified for file descriptors here: https://github.com/jvde-github/airspyone_host.

**AIS-catcher**: <a href="https://github.com/jvde-github/AIS-catcher">https://github.com/jvde-github/AIS-catcher</a>

AIS receiver for RTL SDR dongles, Airspy R2, Airspy Mini, Airspy HF+, HackRF and SDRplay. Licensed under the GPGL license.

## Privacy Policy
At the moment we don't collect any user data. This policy will vary per version so please check this policy for each release.

## To do

- More testing....
- <del>Application crashes when USB device is unplugged whilst in the source selection menu</del>
- Application should automatically switch to SDR source if not playing and device connected
- <del>Application crashes when AirSpy HF+ is disconnected (seems to be a more general issue)</del> Solved.
- <del>Shorter timeouts when connecting to RTL-TCP</del>
- WiFi-only check in case: RTL-TCP streaming or UDP NMEA broadcast to other machines
- <del>Wakelocks and </del>WiFi performance settings, etc...
- <del>Add sync locks for updates</del>
- Count buffer under- and over-runs
- <del>Simple map</del> - radar view
- <del>Simple graphs with statistics</del>
- <del>Start button not properly reset when receiver stops due to timeout?</del>
