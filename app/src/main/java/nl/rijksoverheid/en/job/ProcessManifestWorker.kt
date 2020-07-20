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
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.ProcessManifestResult
import nl.rijksoverheid.en.notifier.NotificationsRepository
import timber.log.Timber
import java.util.concurrent.TimeUnit

private const val WORKER_ID = "process_manifest"
private const val KEY_UPDATE_INTERVAL = "update_interval"

class ProcessManifestWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: ExposureNotificationsRepository,
    private val notificationsRepository: NotificationsRepository
) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val result = repository.processManifest()
        Timber.d("Processing result is $result")
        if (result is ProcessManifestResult.Success) {
            notificationsRepository.cancelSyncIssuesNotification()
            if (inputData.getInt(KEY_UPDATE_INTERVAL, 0) != result.nextIntervalMinutes) {
                queue(applicationContext, result.nextIntervalMinutes)
            }
        }
        // Always mark as success so that retries are only scheduled for exceptions.
        // The next update will be at the next interval.
        return Result.success()
    }

    companion object {
        fun queue(context: Context, intervalMinutes: Int = 240) {
            val request = PeriodicWorkRequestBuilder<ProcessManifestWorker>(
                intervalMinutes.toLong(),
                TimeUnit.MINUTES
            ).apply {
                if (BuildConfig.DEBUG) {
                    setInitialDelay(10, TimeUnit.SECONDS)
                } else {
                    setInitialDelay(intervalMinutes.toLong().div(2), TimeUnit.MINUTES)
                }
            }.setInputData(Data.Builder().putInt(KEY_UPDATE_INTERVAL, intervalMinutes).build())
                .setConstraints(
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
