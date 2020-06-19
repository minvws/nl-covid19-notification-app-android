# Covid19 Notification App - Android

## Introduction
This repository contains the Android App of the Proof of Concept for the Dutch exposure notification app. We provide this code in an early stage so that everyone can follow along as we develop the app, and to solicit feedback. Note that due to the early stage in which we are sharing this code, significant portions of the code might still change. We might add or remove features and code as needed, based on validation and user tests that are conducted partially in parallel to the development.

* The android app is located in the repository you are currently viewing.
* The iOS app can be found here: https://github.com/minvws/nl-covid19-notification-app-ios
* The backend can be found here: https://github.com/minvws/nl-covid19-notification-app-backend
* The designs that are used as a basis to develop the apps can be found here: https://github.com/minvws/nl-covid19-notification-app-design
* The architecture that underpins the development can be found here: https://github.com/minvws/nl-covid19-notification-app-coordination

## Development setup
For communication with the API, the endpoint urls need to be set using a Gradle project property. This can be done by adding the `-P` option on the command line and/or Android Studio compiler
options, or by specifying the properties in your global `gradle.properties`.

Properties that need to be set are `cdnEndpoint` and `apiEndpoint` both need to be fully qualified urls and end with a `/`. For local development use `-PcdnEndpoint=http://<yourip>:5000/cdn/ -PapiEndpoint=http://<yourip>:5000/MobileAppApi/`

## Development & Contribution process

The core team works on the repository in a private fork (for reasons of compliance with existing processes) and will share its work as often as possible.

If you plan to make non-trivial changes, we recommend to open an issue beforehand where we can discuss your planned changes.
This increases the chance that we might be able to use your contribution (or it avoids doing work if there are reasons why we wouldn't be able to use it).

## Disclaimer
Keep in mind that the Google Exposure Notification API is only accessible by verified health authorities. Other devices trying to access the API using the code in this repository will fail to do so.
