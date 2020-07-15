/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.job

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.labtest.LabTestRepository
import nl.rijksoverheid.en.labtest.LabTestRepository.Companion.DECOY_START_HOUR
import timber.log.Timber
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

private const val WORKER_ID = "decoy_scheduler"

class ScheduleDecoyWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: LabTestRepository
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Timber.d("Schedule next decoy sequence")
        repository.scheduleNextDecoyScheduleSequence()
        return Result.success()
    }

    companion object {
        fun queue(context: Context) {
            val request =
                PeriodicWorkRequestBuilder<ScheduleDecoyWorker>(Duration.ofDays(1)).apply {
                    if (!BuildConfig.DEBUG) {
                        val firstRun =
                            if (LocalDateTime.now().toLocalTime().hour < DECOY_START_HOUR) {
                                // schedule to run immediately
                                LocalDateTime.now()
                            } else {
                                // schedule for tomorrow at 1 am
                                LocalDate.now().plusDays(1).atTime(1, 0)
                            }
                        Timber.d("Queue first schedule at $firstRun")
                        val delay =
                            Duration.between(LocalDateTime.now(), firstRun).toMillis()
                                .coerceAtLeast(0)
                        setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    } else {
                        Timber.d("Schedule delay is skipped for debug builds")
                    }
                }
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORKER_ID,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    request.build()
                )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORKER_ID)
        }
    }
}