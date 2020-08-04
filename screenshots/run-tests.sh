#!/bin/sh
### Crude way to create screenshots by running the tests in nl.rijksoverheid.en.screenshots
### Fastlanes' LocaleTestRule changes locale using the given arguments testLocale and endingLocale
cd ..
adb uninstall nl.rijksoverheid.en
./gradlew app:installDevDebug app:installDevDebugAndroidTest
adb shell pm grant nl.rijksoverheid.en android.permission.CHANGE_CONFIGURATION
for l in nl fr en ar bg ro pl de tr es; do
  adb shell am instrument --no-window-animation -w -e testLocale $l -e endingLocale nl_NL -e appendTimestamp true -e package nl.rijksoverheid.en.screenshots nl.rijksoverheid.en.test/androidx.test.runner.AndroidJUnitRunner
done
