/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.job

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.ProcessManifestResult
import java.util.concurrent.TimeUnit

private const val WORKER_ID = "process_manifest"

class ProcessManifestWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: ExposureNotificationsRepository
) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return when (repository.processManifest()) {
            is ProcessManifestResult.Success -> Result.success()
            is ProcessManifestResult.Error -> Result.retry()
        }
    }

    companion object {
        fun queue(context: Context, intervalHours: Int = 4) {
            val request = PeriodicWorkRequestBuilder<ProcessManifestWorker>(
                intervalHours.toLong(),
                TimeUnit.HOURS
            ).setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            ).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORKER_ID,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    request
                )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORKER_ID)
        }
    }
}
