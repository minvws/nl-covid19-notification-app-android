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
import java.util.concurrent.TimeUnit

private const val WORKER_ID = "decoy_sender"

class DecoyWorker(
    context: Context,
    workerParameters: WorkerParameters,
    private val repository: LabTestRepository
) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        try {
            repository.sendDecoyTraffic()
        } catch (ex: Exception) {
            Timber.d(ex, "Error sending decoy traffic")
        }
        return Result.success()
    }

    companion object {
        fun queue(context: Context, delayMills: Long) {
            val request = OneTimeWorkRequestBuilder<DecoyWorker>().setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            ).apply {
                setInitialDelay(delayMills, TimeUnit.MILLISECONDS)
            }

            // if already queued, keep the existing decoy
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORKER_ID, ExistingWorkPolicy.KEEP, request.build())
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORKER_ID)
        }
    }
}
