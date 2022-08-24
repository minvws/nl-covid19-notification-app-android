/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.job

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import nl.rijksoverheid.en.AddExposureResult
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.beagle.debugDrawer
import nl.rijksoverheid.en.notifier.NotificationsRepository
import timber.log.Timber
import java.util.concurrent.TimeUnit

private const val KEY_TEST_EXPOSURE = "test_exposure"
private const val MAX_RUN_ATTEMPTS = 10

/**
 * CoroutineWorker that should run when exposed and will handle the [AddExposureResult].
 */
class ExposureNotificationJob(
    context: Context,
    params: WorkerParameters,
    private val repository: ExposureNotificationsRepository,
    private val notificationsRepository: NotificationsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val testExposure = inputData.getBoolean(KEY_TEST_EXPOSURE, false)
        val result = repository.addExposure(testExposure, debugDrawer.testExposureDaysAgo)
        Timber.d("Add exposure result is $result")
        return when (result) {
            is AddExposureResult.Notify -> {
                notificationsRepository.showExposureNotification(result.dateOfLastExposure, result.notificationReceivedDate)
                RemindExposureNotificationWorker.schedule(applicationContext)
                Result.success()
            }
            is AddExposureResult.Processed -> Result.success()
            is AddExposureResult.Error -> {
                if (runAttemptCount + 1 < MAX_RUN_ATTEMPTS) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        }
    }

    companion object {
        fun showNotification(context: Context, testExposure: Boolean) {
            val request = OneTimeWorkRequestBuilder<ExposureNotificationJob>()
                .setInputData(
                    Data.Builder()
                        .putBoolean(KEY_TEST_EXPOSURE, testExposure)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
