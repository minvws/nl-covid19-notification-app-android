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
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class UploadDiagnosisKeysJob(
    context: Context,
    params: WorkerParameters,
    private val repository: LabTestRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return when (val result = repository.uploadDiagnosticKeysOrDecoy()) {
            is LabTestRepository.UploadDiagnosticKeysResult.Completed -> Result.success()
            is LabTestRepository.UploadDiagnosticKeysResult.Retry -> Result.retry()
            is LabTestRepository.UploadDiagnosticKeysResult.Initial -> {
                scheduleAdditionalKeyUpload(result.dateTime)
                Result.success()
            }
        }
    }

    private fun scheduleAdditionalKeyUpload(uploadAfter: LocalDateTime) {
        val delay = (uploadAfter.toInstant(ZoneOffset.UTC)
            .toEpochMilli() - System.currentTimeMillis()).coerceAtLeast(
            TimeUnit.MILLISECONDS.convert(
                1,
                TimeUnit.HOURS
            )
        )
        Timber.d("Schedule next upload with a delay of $delay ms")
        schedule(applicationContext, delay)
    }

    companion object {
        fun schedule(context: Context, initialDelay: Long = 0) {
            val request = OneTimeWorkRequestBuilder<UploadDiagnosisKeysJob>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                ).apply {
                    if (initialDelay > 0) {
                        setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    }
                }
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork("diagnosis_keys", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
