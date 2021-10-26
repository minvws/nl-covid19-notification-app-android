# COVID-19 Notification App - Android

## Introduction
This repository contains the native Android implementation of the Dutch COVID-19 Notification App CoronaMelder.

* The Android app is located in the repository you are currently viewing.
* The iOS app can be found here: https://github.com/minvws/nl-covid19-notification-app-ios
* The backend can be found here: https://github.com/minvws/nl-covid19-notification-app-backend
* The designs that are used as a basis to develop the apps can be found here: https://github.com/minvws/nl-covid19-notification-app-design
* The architecture that underpins the development can be found here: https://github.com/minvws/nl-covid19-notification-app-coordination

## Feature Overview
This is a general overview of the features that are available in the app:

- **Onboarding**. When the app starts or the first time, the user is informed of the functionality of the app and is asked to give permission for local notifications and the use of the GAEN framework.
- **Treatment perspective**. When you receive an exposure notification, the app will provide information what to do in this situation.
- **Share GGD Key**. If you have tested positive for coronavirus, you can share your unique identifiers publicly to make sure people who came into contact with you during the infectious period are alerted.
- **Pausing**. The user has the option to pause the contact tracing framework for a set number of hours. This is useful when you are in an environment where other measures have been taken to protect people from the virus or in situations where you are aware that you are interacting with a large number of potentially infected individuals (like in Coronavirus testlocations) and you don't want to receive notifications about this in the future.
- **Q&A Section**. In-app information about the functionality of the app, how your privacy is protected and how the app works in detail.
- **Requesting a Coronatest**. Information on how to request a Coronatest with links to related websites or phonenumbers. Note that you can not directly request a test within the app itself.
- **Invite people to use the app**. Allows the user to share a link to [coronamelder.nl](coronamelder.nl) to their friends & family using the android share intent.

Other functions of the app that are automatically performed and are not available through the UI of the app:

- **Contact Tracing** using the GAEN framework. The app regularly downloads published keys from a public API and checks if the user ever came into contact with the device from which these infectious keys originated. This work is performed in regularly scheduled background tasks.
- **Decoy Traffic Generation**. Since the "Share GGD Key" functionality of the app sends data to the API, any network traffic coming from the app could be construed as a sign that the user was infected. Because this presents a potential breach of privacy, the app regularly schedules similar (decoy) calls to the API to mask this traffic.

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
