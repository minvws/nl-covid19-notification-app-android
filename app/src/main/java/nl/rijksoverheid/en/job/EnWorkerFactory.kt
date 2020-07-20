/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.job

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import nl.rijksoverheid.en.factory.createExposureNotificationsRepository
import nl.rijksoverheid.en.factory.createLabTestRepository
import nl.rijksoverheid.en.notifier.NotificationsRepository

class EnWorkerFactory : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            ProcessManifestWorker::class.java.name -> ProcessManifestWorker(
                appContext,
                workerParameters,
                createExposureNotificationsRepository(appContext),
                NotificationsRepository(appContext)
            )
            CheckConnectionWorker::class.java.name -> CheckConnectionWorker(
                appContext,
                workerParameters,
                createExposureNotificationsRepository(appContext),
                NotificationsRepository(appContext)
            )
            ExposureNotificationJob::class.java.name -> ExposureNotificationJob(
                appContext,
                workerParameters,
                createExposureNotificationsRepository(appContext),
                NotificationsRepository(appContext)
            )
            UploadDiagnosisKeysJob::class.java.name -> UploadDiagnosisKeysJob(
                appContext,
                workerParameters
            ) { createLabTestRepository(appContext).uploadDiagnosticKeysIfPending() }
            ScheduleDecoyWorker::class.java.name -> ScheduleDecoyWorker(
                appContext,
                workerParameters,
                createLabTestRepository(appContext)
            )
            DecoyWorker::class.java.name -> DecoyWorker(
                appContext,
                workerParameters,
                createLabTestRepository(appContext)
            )
            else -> null
        }
    }
}
