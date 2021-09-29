/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.job

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import nl.rijksoverheid.en.labtest.LabTestRepository
import nl.rijksoverheid.en.notifier.NotificationsRepository
import timber.log.Timber

/**
 * CoroutineWorker to upload diagnosis keys.
 */
class UploadDiagnosisKeysJob(
    context: Context,
    params: WorkerParameters,
    private val notificationsRepository: NotificationsRepository,
    private val uploadTask: suspend () -> LabTestRepository.UploadDiagnosticKeysResult
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return when (uploadTask()) {
            is LabTestRepository.UploadDiagnosticKeysResult.Success -> Result.success()
            is LabTestRepository.UploadDiagnosticKeysResult.Expired -> {
                notificationsRepository.showUploadKeysFailedNotification()
                Result.success()
            }
            is LabTestRepository.UploadDiagnosticKeysResult.Retry -> Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            Timber.d("Schedule uploading of keys")
            val request = OneTimeWorkRequestBuilder<UploadDiagnosisKeysJob>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                ).build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork("diagnosis_keys", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
