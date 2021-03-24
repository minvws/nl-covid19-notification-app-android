/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.debug.DebugNotification
import nl.rijksoverheid.en.job.ExposureNotificationJob
import timber.log.Timber

class ExposureNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED -> {
                ExposureNotificationJob.showNotification(context, false)
            }
            ExposureNotificationClient.ACTION_EXPOSURE_NOT_FOUND -> {
                Timber.d("No exposure new detected")
            }
            DebugNotification.ACTION_TEST_EXPOSURE -> {
                if (BuildConfig.FEATURE_DEBUG_NOTIFICATION) {
                    ExposureNotificationJob.showNotification(context, true)
                }
            }
        }
    }
}
