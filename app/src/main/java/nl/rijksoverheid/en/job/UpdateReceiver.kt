/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.job

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.factory.createExposureNotificationsRepository

/**
 * App Update Receiver
 */
class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        // Reschedule background jobs when updating to 2.0.0 to make sure CleanupWorker gets scheduled
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED && BuildConfig.VERSION_NAME == "2.0.0") {
            val exposureNotificationsRepository = createExposureNotificationsRepository(context)
            GlobalScope.launch {
                exposureNotificationsRepository.rescheduleBackgroundJobs()
            }
        }
    }
}
