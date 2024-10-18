
# ADB⚡OTG

This project is a fork of the [ADB⚡OTG](https://github.com/KhunHtetzNaing/ADB-OTG) repository, utilizing the 
[adb library](https://github.com/wuxudong/flashbot) developed by [wuxudong](https://github.com/wuxudong).

## Overview
#### Run ADB commands from an android device [Without Rooting Device]
This app is designed to assist victims of Intimate Partner Violence (IPV), who feel unsafe using their devices. It provides a secure and comprehensive way to detect and manage potential spyware and privacy risks present on their devices.

## Features
- #### Device Scan
  - [x] **App Detection**: Scans all apps on the device and compares them against a predefined remote CSV file of dual-use and spyware applications.
  - [x] **Ranking System**: Ranks apps from most harmful to least:
      - Red: Downloaded from an external location (off-store)
      - Yellow: Identified as pure spyware
      - Light Blue: Identified as dual-use (e.g., Location 360, and many parental control apps)
  - [x] **Secure Store Launch**: Provides a button for each listed app to launch the secure store it was downloaded from.
  - [x] **In-Device Settings Link**: Links to the in-device settings for each app so the user can check permissions.
  - [x] **Permission Risks**: if an app has certain permissions activated, the app lists the risks of those permissions and provides recommendations on whether to keep them on or off.

#### Privacy Scan
- [x] **Google Privacy Checkup**: Links to in-device Google privacy checkup settings.
- [x] **Social Media Settings**: Links to the settings pages of popular social media apps installed on the device, with recommendations on which settings to deactivate for enhanced privacy.

#### ADB Feature
- [x] **Remote Scanning**: Allows a source device (with the app installed) to connect remotely (via USB) to a target device.
- [ ] **Data Output**: Outputs all scan data from the target device to the source device.
- [ ] **Risk Mitigation**: Enables the victim to get help without having to download anything directly on their device, reducing the risk of alerting their abuser.


## Technology Stack:
- Java: Used for native Android application development.
- XML: Used for Android layout and resource files.
- Gradle: Used for build scripts.
- AndroidX Libraries: to provide backward-compatible versions of Android components.
- ADBLib: A library for ADB functionalities.

## Useful Commands
- ```adb devices```: shell command that enables access to a connected Android device.
- ```adb tcpip 5555```: shell command to enable tcp/ip on the network.
- ```adb shell ip -f inet addr show wlan0```: shell command to get the IP address of the target device.
- ```adb connect <IP_ADDRESS>:5555```: shell command to connect to the target device.
- ```adb shell pm list packages ```: shell command to get list of app packages from the device.
- ```flutter clean```: clear the build cache.
- ```flutter build apk```: build app apk for downloading the app.
- ```flutter intall```: install app on specified device.
- ```dumpsys```: dumps diagnostic information about the status of system services
- ```pm list packages```: lists all packages on the device.
