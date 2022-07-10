# AIS-catcher for Android - A multi-platform AIS receiver 
This Android App helps to change your Android device into a dual channel AIS receiver that can be used to pick up AIS signals from nearby vessels, even if offline!
The app directly accesses a Software Defined Radio USB device and can send received messages via UDP to plotting Apps like [Boat Beacon](https://pocketmariner.com/mobile-apps/boatbeacon/).

If you want to download AIS-catcher from the Google Play store and have a Google account, please get in contact with  [me](mailto:jvde.github@gmail.com) and I will provide you with a link to
the store location for the internal test (or download the APK file below). 
 
The requirements to recieve AIS signals are: a RTL-SDR dongle (or alternatively an Airspy, Airspy HF+ or RTL-TCP/SpyServer connection), a simple antenna, 
an Android device with USB connector and an OTG cable to connect the dongle with your Android device. Please note that this might not work
on all Android devices. AIS-catcher only receives and processes signals and then forwards the messages over UDP. To make it a bit more interesting you would need an App like
[Boat Beacon](https://pocketmariner.com/mobile-apps/boatbeacon/) or [OpenCPN](https://play.google.com/store/apps/details?id=org.opencpn.opencpn_free) to map the received vessel information.
Also, do not forget to set the frequency offset for your RTL-SDR device. 

And one more thing, you need to be in a region where there are ships broadcasting AIS signals, e.g. near the water.

<p align="center">
<img src="https://raw.githubusercontent.com/jvde-github/AIS-catcher/media/media/equipment.jpg" width=80% height=80%></p>

### Purpose and Disclaimer

```AIS-catcher for Android```  is created for research and educational purposes under the GPL license.
This program comes with **ABSOLUTELY NO WARRANTY**; This is free software, and you are welcome to redistribute it
under certain conditions. For details see [https://github.com/jvde-github/AIS-catcher-for-Android](https://github.com/jvde-github/AIS-catcher-for-Android).

It is a hobby project and not tested and designed for reliability and correctness. 
You can play with the software but it is the user's responsibility to use it prudently. So,  **DO NOT** rely upon this software in any way including for navigation 
and/or safety of life or property purposes.
There are variations in the legislation concerning radio reception in the different administrations around the world. 
It is your responsibility to determine whether or not your local administration permits the reception and handling of AIS messages from ships and you can have this App on your phone. 
It is specifically forbidden to use this software for any illegal purpose whatsoever. 
The software is intended for use **only in those regions** where such use **is permitted**.

The maximum time for the receiver is set to two minutes for this reason.

## Latest news: Android version available as [APK](https://drive.google.com/file/d/1HDm39szX_kF-Bg6KKruabFoWmzveji0w/view?usp=sharing)

Feedback is very much appreciated by mailing [me](mailto:jvde.github@gmail.com) or sharing in the Issue section. If you have any prior version, I suggest to remove this first.

For a video of a field test of an early version [see YouTube](https://www.youtube.com/shorts/1ArB7GL_yV8). 

## Tutorial: connecting AIS-catcher to OpenCPN and BoatBeacon

### The main screen
The main screen of AIS-catcher looks as follows:
<p align="center">
<img src="https://github.com/jvde-github/AIS-catcher/blob/a84ea19fb6198a292390a5b7ce64d06f5070f8a8/media/main_screen%5B1%5D.jpg" width=20% height=20%>
</p>

### Setting up the connection with OpenCPN and/or BoatBeacon
First we are going to set up the outward connections to BoatBeacon and OpenCPN. For this I will use port 10110 for BoatBeacon and 10111 for OpenCPN. Press the 3 vertical dots on the top left and select the settings option. Scroll to the bottom. Activate the two UDP output channels and set as follows:
<p align="center">
<img src="https://github.com/jvde-github/AIS-catcher/blob/d7786eb75a15bcf230bf5e3a7f842de32e05f2bf/media/settings_UDP%5B1%5D.jpg" width=20% height=20%>
</p>

### Setting up OpenCPN
Next we start OpenCPN and click on Settings (top left icon) and choose the Connections tab. We  add a Network connection (UDP at address 0.0.0.0 dataport 10111). You can chose "Show NMEA Debug Window" as extra option that shows the messages that OpenCPN receives from AIS-catcher.
<p align="center">
<img src="https://github.com/jvde-github/AIS-catcher/blob/90d0cb0cedc1220321c474621cb13eb787d6087d/media/OpenCPN_UDP_settings%5B1%5D.jpg" width=20% height=20%>
</p>

### Granting AIS-catcher access to the USB dongle
Next we set up the RTL-SDR dongle. Connect the dongle with your Android device and if all is well you should be asked if AIS-catcher can get access to the USB device. Accept.

<p align="center">
<img src="https://github.com/jvde-github/AIS-catcher/blob/eb25b78295fe2320683bf569c346a2157e8b8c69/media/grant_access_to_USB%5B1%5D.jpg" width=20% height=20%>
</p>

### Setting up the RTL-SDR dongle
Next go back to the "Settings" menu via the 3 vertical dots and set the RTL-SDR settings:

<p align="center">
<img src="https://github.com/jvde-github/AIS-catcher/blob/b9093aae8ee7545b6dd5f7629e546691c9bce6a8/media/setting_RTL-SDR%5B2%5D.jpg" width=20% height=20%>
</p>
These settings should be ok but don't forget to set the frequency correction in PPM if needed for your device.

### Selecting the device for receiving AIS messages
In the Main screen select the source on the right in the bottom navigation bar. Select the RTL-SDR device:
<p align="center">
<img src="https://github.com/jvde-github/AIS-catcher/blob/b3a5d1f26a4452b8707b6d79408b6e6c39d30889/media/slect_device%5B1%5D.jpg" width=20% height=20%>
</p>

### Running AIS-catcher
In the main screen, select Play on the left in the bottom navigation bar. This should start the run. The navigation tabs allow you to see different statistics (like message count, status messages from the receiver and NMEA lines).
<p align="center">
<img src="https://github.com/jvde-github/AIS-catcher/blob/1c2b33d6b7c9df0d5a85e3224a196481d7df92ba/media/running_main_screen%5B1%5D.jpg" width=20% height=20%>
<img src="https://github.com/jvde-github/AIS-catcher/blob/1c2b33d6b7c9df0d5a85e3224a196481d7df92ba/media/running_console%5B1%5D.jpg" width=20% height=20%>
<img src="https://github.com/jvde-github/AIS-catcher/blob/1c2b33d6b7c9df0d5a85e3224a196481d7df92ba/media/running_nmea_screen%5B1%5D.jpg" width=20% height=20%>
<img src="https://github.com/jvde-github/AIS-catcher/blob/1c2b33d6b7c9df0d5a85e3224a196481d7df92ba/media/running_OCPN%5B1%5D.jpg" width=20% height=20%>
</p>

AIS-catcher will run in the background as a foreground service. 

## To do

- More testing....
- <del>Application crashes when Airspy HF+ is disconnected (seems to be a more general issue)</del> Solved.
- <del>Shorter timeouts when connecting to RTL-TCP</del>
- WiFI-only check in case: RTL-TCP streaming or UDP NMEA broadcast to other machines
- Wakelocks and WiFI performance settings, etc...
- Add sync locks for updates
- Count bufer under- and over-runs
- Simple map - radar view
- Simple graphs with statistics
- Start button not properly reset when receiver stops due to timeout?
