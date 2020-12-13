/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import nl.rijksoverheid.en.enapi.StatusResult
import nl.rijksoverheid.en.factory.createExposureNotificationsRepository
import nl.rijksoverheid.en.factory.createSettingsRepository
import nl.rijksoverheid.en.notifier.NotificationsRepository
import timber.log.Timber

class ExposureNotificationsStateChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ExposureNotificationClient.ACTION_SERVICE_STATE_UPDATED) {
            val enabled =
                intent.getBooleanExtra(ExposureNotificationClient.EXTRA_SERVICE_STATE, false)
            Timber.d("Exposure notification service state changed: $enabled")
            if (enabled) {
                val token = goAsync()
                val settingsRepository = createSettingsRepository(context)
                val exposureNotificationsRepository = createExposureNotificationsRepository(context)
                GlobalScope.launch {
                    // end pause when the app is in working state
                    if (exposureNotificationsRepository.getCurrentStatus() == StatusResult.Enabled) {
                        exposureNotificationsRepository.requestEnableNotifications()
                        settingsRepository.clearExposureNotificationsPaused()
                        NotificationsRepository(context).clearAppPausedNotification()
                    }
                    token.finish()
                }
            }
        }
    }
}
