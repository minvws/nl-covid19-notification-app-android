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
import nl.rijksoverheid.en.enapi.StatusResult
import nl.rijksoverheid.en.notifier.NotificationsRepository
import timber.log.Timber
import java.util.concurrent.TimeUnit

private const val WORKER_ID = "check_connection"

class CheckConnectionWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: ExposureNotificationsRepository,
    private val notificationsRepository: NotificationsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Timber.d("Check if key processing is overdue or EN is disabled")
        if (repository.getCurrentStatus() == StatusResult.Disabled) {
            Timber.d("EN is disabled")
            notificationsRepository.showAppInactiveNotification()
        } else if (repository.keyProcessingOverdue) {
            Timber.d("Key processing is overdue")
            SyncIssuesReceiver.schedule(applicationContext)
        }
        return Result.success()
    }

    companion object {
        fun queue(context: Context) {
            val request = PeriodicWorkRequestBuilder<CheckConnectionWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(1, TimeUnit.MINUTES).build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORKER_ID,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    request
                )
        }

        fun cancel(context: Context) {
            SyncIssuesReceiver.cancel(context)
            WorkManager.getInstance(context).cancelUniqueWork(WORKER_ID)
        }
    }
}
