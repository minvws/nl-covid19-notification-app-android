/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.job.ExposureCleanupWorker

/**
 * App Update Receiver
 */
class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val updatePrefs = UpdatePrefs(context)

        // Reschedule background jobs when updating to versions higher than 1.3.0 to make sure ExposureCleanupWorker gets scheduled
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            when {
                BuildConfig.VERSION_CODE > VERSION_CODE_1_3_0 && // Current version code is higher than that of 1.3.0
                    updatePrefs.lastVersionUpdated <= VERSION_CODE_1_3_0 // Version the user is updating from is 1.3.0 or lower.
                -> ExposureCleanupWorker.queue(context)
            }
            // This intent is only received once, so update updatePrefs.lastVersionUpdated to current version code.
            updatePrefs.lastVersionUpdated = BuildConfig.VERSION_CODE
        }
    }

    companion object {
        const val VERSION_CODE_1_3_0 = 92080
    }
}
