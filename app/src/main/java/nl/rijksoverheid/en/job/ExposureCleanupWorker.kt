/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.job

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import nl.rijksoverheid.en.ExposureNotificationsRepository
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * CoroutineWorker that will run once a day to cleanup old previously known exposures.
 */
class ExposureCleanupWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: ExposureNotificationsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Timber.d("Cleanup outdated previously known expiration dates")
        // try / catch this to be sure the worker always returns success and keeps being scheduled
        try {
            repository.cleanupPreviouslyKnownExposures()
        } catch (e: Exception) {
            Timber.w(e, "Unexpected error occurred")
            return Result.failure()
        }
        return Result.success()
    }

    companion object {
        private const val WORKER_ID = "exposure_cleanup"

        fun queue(context: Context) {
            val request = PeriodicWorkRequestBuilder<ExposureCleanupWorker>(1, TimeUnit.DAYS).build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORKER_ID,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORKER_ID)
        }
    }
}
