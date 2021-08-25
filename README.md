# COVID-19 Notification App - Android

## Introduction
This repository contains the native Android implementation of the Dutch COVID-19 Notification App CoronaMelder.

* The Android app is located in the repository you are currently viewing.
* The iOS app can be found here: https://github.com/minvws/nl-covid19-notification-app-ios
* The backend can be found here: https://github.com/minvws/nl-covid19-notification-app-backend
* The designs that are used as a basis to develop the apps can be found here: https://github.com/minvws/nl-covid19-notification-app-design
* The architecture that underpins the development can be found here: https://github.com/minvws/nl-covid19-notification-app-coordination

## Local development setup
For communication with the [standalone server][1] with the `devDebug` flavor, the endpoint urls need to be set using a Gradle project property. This can be done by adding the `-P` option on the command line and/or Android Studio compiler
options, or by specifying the properties in your global `gradle.properties`.

Properties that need to be set are `cdnEndpoint` and `apiEndpoint` both need to be fully qualified urls and end with a `/`.

## Development & Contribution process

The development team works on the repository in a private fork (for reasons of compliance with existing processes) and shares its work as often as possible.

If you plan to make non-trivial changes, we recommend to open an issue beforehand where we can discuss your planned changes.
This increases the chance that we might be able to use your contribution (or it avoids doing work if there are reasons why we wouldn't be able to use it).

## Supported devices

The Android app is supported on devices with API level 23 and above, that is Android 6.0 and above, that include support for Bluetooth Low Energy.

The app uses the Google Exposure Notification API, therefore Google Play services is required. The app supports Google Play services version 202665000 and above.

## Disclaimer
Keep in mind that the Google Exposure Notification API is only accessible by verified health authorities. Other devices trying to access the API using the code in this repository will fail to do so.

[1]:https://github.com/minvws/nl-covid19-notification-app-backend
