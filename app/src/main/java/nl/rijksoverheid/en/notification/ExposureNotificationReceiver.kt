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
import nl.rijksoverheid.en.job.ExposureNotificationJob
import timber.log.Timber

class ExposureNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED) {
            val testExposure = intent.getBooleanExtra(KEY_IS_TEST_EXPOSURE, false)
            ExposureNotificationJob.showNotification(context, testExposure)
        } else if (intent.action == ExposureNotificationClient.ACTION_EXPOSURE_NOT_FOUND) {
            Timber.d("No exposure new detected")
        }
    }

    companion object {
        const val KEY_IS_TEST_EXPOSURE = "is_test_exposure"
    }
}
