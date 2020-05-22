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
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.ImportTemporaryExposureKeysResult
import nl.rijksoverheid.en.factory.createExposureNotificationsRepository
import timber.log.Timber
import java.util.concurrent.TimeUnit

class DownloadDiagnosisKeysWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val repository: ExposureNotificationsRepository =
        createExposureNotificationsRepository(context)

    override suspend fun doWork(): Result {
        return when (repository.importTemporaryExposureKeys()) {
            ImportTemporaryExposureKeysResult.Success -> {
                Timber.d("Imported keys")
                Result.success()
            }
            is ImportTemporaryExposureKeysResult.Error -> {
                Timber.w("Error while importing keys")
                Result.failure()
            }
        }
    }

    companion object {
        fun queue(context: Context) {
            val request = OneTimeWorkRequestBuilder<DownloadDiagnosisKeysWorker>().setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            ).setInitialDelay(5, TimeUnit.SECONDS).build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("download_keys", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
