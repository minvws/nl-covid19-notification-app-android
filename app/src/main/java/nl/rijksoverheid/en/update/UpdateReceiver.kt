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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.factory.RepositoryFactory
import nl.rijksoverheid.en.job.ExposureCleanupWorker
import timber.log.Timber

/**
 * App Update Receiver
 */
class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val updatePrefs = UpdatePrefs(context)

        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            if (BuildConfig.VERSION_CODE > VERSION_CODE_1_3_0 &&
                updatePrefs.lastVersionUpdated <= VERSION_CODE_1_3_0
            ) {
                Timber.d("Run 1.3.0+ migration")
                // Reschedule background jobs when updating to versions higher than 1.3.0 from 1.3.0 or below
                // to make sure ExposureCleanupWorker gets scheduled
                ExposureCleanupWorker.queue(context)
            }

            if (BuildConfig.VERSION_CODE > VERSION_CODE_2_5_7 &&
                updatePrefs.lastVersionUpdated <= VERSION_CODE_2_5_7
            ) {
                Timber.d("Run 2.5.7+ migration")
                // Rerun processManifest job to ensure all disabled app have also disabled the framework
                rescheduleBackgroundJobs(context)
            }

            // This intent is only received once, so update updatePrefs.lastVersionUpdated to current version code.
            updatePrefs.lastVersionUpdated = BuildConfig.VERSION_CODE
        }
    }

    private fun rescheduleBackgroundJobs(context: Context) {
        val exposureNotificationsRepository = RepositoryFactory.createExposureNotificationsRepository(context)

        val async = goAsync()
        MainScope().launch {
            exposureNotificationsRepository.rescheduleBackgroundJobs()
            async.finish()
        }
    }

    companion object {
        const val VERSION_CODE_1_3_0 = 92080
        const val VERSION_CODE_2_5_7 = 156740
    }
}
