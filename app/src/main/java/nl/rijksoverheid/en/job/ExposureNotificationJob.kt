/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.job

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import nl.rijksoverheid.en.AddExposureResult
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.notifier.NotificationsRepository

private const val KEY_TEST_EXPOSURE = "test_exposure"

class ExposureNotificationJob(
    context: Context,
    params: WorkerParameters,
    private val repository: ExposureNotificationsRepository,
    private val notificationsRepository: NotificationsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val testExposure = inputData.getBoolean(KEY_TEST_EXPOSURE, false)
        val result = repository.addExposure(testExposure)
        if (result is AddExposureResult.Notify) {
            notificationsRepository.showExposureNotification(result.daysSinceExposure)
            RemindExposureNotificationWorker.schedule(applicationContext)
        }
        return Result.success()
    }

    companion object {
        fun showNotification(context: Context, testExposure: Boolean) {
            val request = OneTimeWorkRequestBuilder<ExposureNotificationJob>()
                .setInputData(
                    Data.Builder()
                        .putBoolean(KEY_TEST_EXPOSURE, testExposure)
                        .build()
                ).build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
