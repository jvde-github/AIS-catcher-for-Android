# AIS-catcher for Android - A multi-platform AIS receiver 
This Android App helps to change your Android device into a dual channel AIS receiver that can be used to pick up AIS signals from nearby vessels, even if offline!
The App directly accesses a Software Defined Radio USB device and can send received messages via UDP to plotting Apps like [Boat Beacon](https://pocketmariner.com/mobile-apps/boatbeacon/) or [OpenCPN](https://play.google.com/store/apps/details?id=org.opencpn.opencpn_free). A lightweight AIS receiver system when travelling.

The following screenshot was taken in July 2022 with AIS-catcher receiving signals for a few minutes on a Samsung Galaxy S6 on a beach near The Hague with a simple antenna. Ship positions are plotted with the BoatBeacon app.
 
<p align="center">
<img src="https://github.com/jvde-github/AIS-catcher/blob/152e5460fd938fb701d988f01deccdfa6192443d/media/Screenshot_BoatBeacon%5B1%5D.jpg" width=40% height=40%>
</p>

Below you can find a link to the APK file. For now I have decided not to make AIS-catcher available in the Play Store, this is too cumbersome for a small and free app. The only way
to install it is by building yourself or install a pre-build APK file.
 
The requirements to receive AIS signals are: a RTL-SDR dongle (or alternatively an AirSpy Mini/R2/HF+), a simple antenna, 
an Android device with USB connector and an OTG cable to connect the dongle with your Android device. 
AIS-catcher only receives and processes signals and then forwards the messages over UDP. To make it a bit more interesting you would need an App like
[Boat Beacon](https://pocketmariner.com/mobile-apps/boatbeacon/) or [OpenCPN](https://play.google.com/store/apps/details?id=org.opencpn.opencpn_free) to map the received vessel information.

And one more thing, you need to be in a region where there are ships broadcasting AIS signals, e.g. near the water.

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

You can download AIS-catcher-for-Android in the [Release section](https://github.com/jvde-github/AIS-catcher-for-Android/releases) in the form of an APK-file. 
If you get an error message, please delete any previous version first. Feedback is very much appreciated by mailing [me](mailto:jvde.github@gmail.com) or sharing in the Issue section. The latest development version is available [here](https://github.com/jvde-github/AIS-catcher-for-Android/actions) as artifact of an automatic build.

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

## To do

- More testing....
- <del>Application crashes when AirSpy HF+ is disconnected (seems to be a more general issue)</del> Solved.
- <del>Shorter timeouts when connecting to RTL-TCP</del>
- WiFi-only check in case: RTL-TCP streaming or UDP NMEA broadcast to other machines
- <del>Wakelocks and </del>WiFi performance settings, etc...
- <del>Add sync locks for updates</del>
- Count buffer under- and over-runs
- Simple map - radar view
- Simple graphs with statistics
- <del>Start button not properly reset when receiver stops due to timeout?</del>
