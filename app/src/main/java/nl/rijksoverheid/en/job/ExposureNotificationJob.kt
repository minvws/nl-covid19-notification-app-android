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

private const val KEY_TOKEN = "token"

class ExposureNotificationJob(
    context: Context,
    params: WorkerParameters,
    private val repository: ExposureNotificationsRepository,
    private val notificationsRepository: NotificationsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val token = inputData.getString(KEY_TOKEN)!!
        val result = repository.addExposure(token)
        if (result is AddExposureResult.Notify) {
            notificationsRepository.showExposureNotification(result.daysSinceExposure)
        }
        return Result.success()
    }

    companion object {
        fun showNotification(context: Context, token: String) {
            val request = OneTimeWorkRequestBuilder<ExposureNotificationJob>()
                .setInputData(
                    Data.Builder()
                        .putString(KEY_TOKEN, token)
                        .build()
                ).build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
