# AIS-catcher for Android - A multi-platform AIS receiver 
This Android App helps to change your Android device into a dual channel AIS receiver that can be used to pick up AIS signals from nearby vessels, even if offline!
For this you need a USB radio receiver, like a RTL-SDR dongle, and a so-called OTG cable to be able to connect USB devices to your phone.
The app directly accesses the RTL-SDR via a USB OTG connection and can send received messages via UDP to plotting Apps like [Boat Beacon](https://pocketmariner.com/mobile-apps/boatbeacon/).

If you want to download AIS-catcher from the Google Play store and have a Google account, please get in contact with  [me](mailto:jvde.github@gmail.com) and I will provide you with a link to
the store location for the internal test (or download the APK file below). 
 
The requirements to recieve AIS signals are: a RTL-SDR dongle (or alternatively an Airspy, Airspy HF+ or RTL-TCP connection), a simple antenna, 
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

## Latest news: Android version available for testing

It is easy to give AIS-catcher a try on your Android device and if you are familiar with AIS and/or AIS-catcher for Linux/Windows the usage should be very intuitive:
<p align="center">
<img src="https://raw.githubusercontent.com/jvde-github/AIS-catcher/media/media/AIScatcher%20for%20Android%20screenshot%201.png" width=20% height=20%>
<img src="https://raw.githubusercontent.com/jvde-github/AIS-catcher/media/media/AIScatcher%20for%20Android%20screenshot%202.png" width=20% height=20%>
</p>

For testing purposes you can download a [APK file from my Google drive](https://drive.google.com/file/d/1HDm39szX_kF-Bg6KKruabFoWmzveji0w/view?usp=sharing) (I might have to give you access). 
After downloading the file on your phone, just tap to install. After that you should be good to go. Feedback is very much appreciated by mailing [me](mailto:jvde.github@gmail.com) 
or sharing in the Issue section.

For a video of a field test of an early version [see YouTube](https://www.youtube.com/shorts/1ArB7GL_yV8). Hopefully in the Play Store by the end of this Summer.

More to come....


## To do

- More testing....
- <del>Application crashes when Airspy HF+ is disconnected (seems to be a more general issue)</del> Solved.
- <del>Shorter timeouts when connecting to RTL-TCP</del>
- WiFI-only check when using RTL-TCP our external UDP NMEA broadcast
- Wakelocks etc...
- Count bufer under- and over-runs
- Simple map - radar view
- Simple graphs with statistics
