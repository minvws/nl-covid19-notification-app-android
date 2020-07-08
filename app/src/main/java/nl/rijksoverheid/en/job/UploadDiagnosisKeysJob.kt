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
import java.util.concurrent.TimeUnit

class UploadDiagnosisKeysJob(
    context: Context,
    params: WorkerParameters,
    private val uploadTask: suspend () -> LabTestRepository.UploadDiagnosticKeysResult
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return when (val result = uploadTask()) {
            is LabTestRepository.UploadDiagnosticKeysResult.Success -> Result.success()
            is LabTestRepository.UploadDiagnosticKeysResult.Retry -> Result.retry()
        }
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
