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
- **App Config Notifications**. Provides the possibility to schedule a notification by configuring it in the app config.

Other functions of the app that are automatically performed and are not available through the UI of the app:

- **Contact Tracing** using the GAEN framework. The app regularly downloads published keys from a public API and checks if the user ever came into contact with the device from which these infectious keys originated. This work is performed in regularly scheduled background tasks.
- **Decoy Traffic Generation**. Since the "Share GGD Key" functionality of the app sends data to the API, any network traffic coming from the app could be construed as a sign that the user was infected. Because this presents a potential breach of privacy, the app regularly schedules similar (decoy) calls to the API to mask this traffic.

## Supported devices

The Android app is supported on devices with API level 23 and above, that is Android 6.0 and above, that include support for Bluetooth Low Energy.

The app uses the Google Exposure Notification API, therefore Google Play services is required. The app supports Google Play services version 202665000 and above.


## Development & Contribution process

The development team works on the repository in a private fork (for reasons of compliance with existing processes) and shares its work as often as possible.

If you plan to make non-trivial changes, we recommend to open an issue beforehand where we can discuss your planned changes.
This increases the chance that we might be able to use your contribution (or it avoids doing work if there are reasons why we wouldn't be able to use it).

Git commits must be signed https://docs.github.com/en/github/authenticating-to-github/signing-commits

## Development Setup

Google Account on (test)device must be on the allow list for Exposure Notifications to enable debug settings and enable “Bypass app signature check” to be able to run the app with a debug signature.
"Enable diagnostics key file signature check" needs to be enabled to catch any errors related to the TEK file signatures on the server.

## Build types and flavors
The Android project has a couple of flavors to target the specific environments. All flavors have a release and debug build type, except for the production flavor (to prevent debugging on production, this can be turned on in `build.gradle` when needed).

The build flavors control feature flags that are configured as BuildConfig fields.
Currently the `app` module and the `api` module use these flavors.


| Flavor | Features |
|--------|----------|
|dev|SSL pinning and response signatures *disabled*. Endpoints need to be set using Gradle properties `cdnEndpoint` and `apiEndpoint`, defaulting to localhost. This configuration can be used when inspecting traffic through a proxy and when running unit tests.|
|tst|External log file enabled on the external storage application directory for debug purposes. A debug notification is enabled to allow for testing the notification state. The "secure screen" feature preventing screenshots on the status and notification screens is disabled. Endpoints are set to the tst environment|
|acc|Production-like build with external log file enabled on the external storage application directory for testing purposes. Endpoints are set to the acc environment|
|prod| Production build with all security features enabled|

## Core app functionality / starting points
This section highlights the key functionalities in relation to the code for getting started.

### Exposure notification API
CoronaMelder has a (partial) abstraction `ExposureNotificationApi` around the Google Exposure Notifications API that converts the task based async API in Play Services to Kotlin `suspend` functions. This also aids testing by using `FakeExposureNotificationApi` in various tests.

`NearbyExposureNotificationApi` is the main implementation, wrapping the Google `ExposureNotificationClient`.

The Google Exposure Notification API is provided in the `play-services-nearby-api` module as an aar file.

### ExposureNotificationsRepository
`ExposureNotificationsRepository` deals with everything related to exposure notifications through `ExposureNotificationApi` and is used throughout the app. The main functions are
* Reporting the API status (enabled, disabled and checking of preconditions such as if bluetooth and location services are enabled)
* Changing the API status (enable requesting consent, disabling)
* Processing diagnostic keys from the server
* Processing callbacks from this processing by recording exposures that reach the risk level threshold

#### Processing of manifest and TEK files
The `processManifest` is periodically run from `ProcessManifestWorker` and will fetch the latest manifest from the server, check if the app is decommissioned or needs to be updated.

The `processExposureKeySets` method manages the downloading of new exposure key files from the cdn. The goal is to attempt to process all files available and it will keep going (but recording errors) if a file fails to download or doesn't pass signature verification. The processed exposure key sets are stored in encrypted shared preferences so that they are only processed once.

For processing diagnostic keys, a token is generated. If an exposure is detected `ExposureBroadcastReceiver` will be called with that token. This will kick off `ExposureNotificationJob` which will then call back into `ExposureNotificationRepository` `addExposure()` to determine if the user should be notified or not.

Note that we're currently using the deprecated "v1" API mode.

### Network setup
The `api` module contains all network related (Retrofit) services. There are a couple notable interceptors configured on the `OkHttp` instance which are configured using annotations on the Retrofit service interfaces.

#### SignedResponseInterceptor
This interceptor deals with verifying responses from the server that are signed. In a signed response, the response is actually a zip file containing `content.bin` and `content.sig`. The latter contains the cryptographic signature for the content file. The interceptor uses `ResponseSignatureValidator` to validate the signature and then transforms the response to a "normal" json response (the `content.bin` file). Methods annotated with `@SignedResponse` will use this interceptor.

#### PaddedRequestInterceptor
This interceptor is used on the api endpoints to pad the request with additional bytes. This prevents an attacker from determining the type of request by looking at the network traffic.
This interceptor requires `RequestSize` as a parameter, which is set using the `@Tag` annotation on the Retrofit interface. It also requires the body of the request to have a `padding` json field. Methods annotated with `@PaddedRequest` will use this interceptor.

#### SignedBodyInterceptor
This interceptor is used to calculate the HMAC signature when posting TEKs to the `postKeys` endpoint. The secret is passed using the `HmacSecret` data class, set as a `@Tag` on the retrofit interface. The calculated hmac will be base64 encoded and added as a query parameter.

## Disclaimer
Keep in mind that the Google Exposure Notification API is only accessible by verified health authorities. Other devices trying to access the API using the code in this repository will fail to do so.

[1]:https://github.com/minvws/nl-covid19-notification-app-backend
