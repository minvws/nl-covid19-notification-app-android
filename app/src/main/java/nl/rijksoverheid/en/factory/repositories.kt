/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.factory

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.nearby.Nearby
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import nl.rijksoverheid.en.AppLifecycleManager
import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.api.CdnService
import nl.rijksoverheid.en.api.LabTestService
import nl.rijksoverheid.en.enapi.NearbyExposureNotificationApi
import nl.rijksoverheid.en.job.CheckConnectionWorker
import nl.rijksoverheid.en.job.ProcessManifestWorker
import nl.rijksoverheid.en.job.ProcessManifestWorkerScheduler
import nl.rijksoverheid.en.job.UploadDiagnosisKeysJob
import nl.rijksoverheid.en.labtest.LabTestRepository
import nl.rijksoverheid.en.onboarding.GooglePlayServicesUpToDateChecker
import nl.rijksoverheid.en.onboarding.OnboardingRepository

// cached service instance
private var cdnService: CdnService? = null
private var labTestService: LabTestService? = null
private var notificationPreferences: SharedPreferences? = null

fun createExposureNotificationsRepository(context: Context): ExposureNotificationsRepository {
    val service = cdnService ?: CdnService.create(context).also { cdnService = it }

    return ExposureNotificationsRepository(
        context,
        NearbyExposureNotificationApi(context, Nearby.getExposureNotificationClient(context)),
        service,
        createSecurePreferences(context),
        object : ProcessManifestWorkerScheduler {
            override fun schedule(intervalMinutes: Int) {
                ProcessManifestWorker.queue(context, intervalMinutes)
                CheckConnectionWorker.queue(context)
            }

            override fun cancel() {
                ProcessManifestWorker.cancel(context)
                CheckConnectionWorker.cancel(context)
            }
        },
        createAppLifecycleManager(context)
    )
}

private fun createGooglePlayServicesChecker(context: Context): GooglePlayServicesUpToDateChecker =
    {
        val result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        result == ConnectionResult.SUCCESS || result == ConnectionResult.SERVICE_UPDATING
    }

fun createOnboardingRepository(
    context: Context,
    checker: GooglePlayServicesUpToDateChecker = createGooglePlayServicesChecker(context)
): OnboardingRepository {
    return OnboardingRepository(
        context.getSharedPreferences("${BuildConfig.APPLICATION_ID}.onboarding", 0),
        checker
    )
}

fun createLabTestRepository(context: Context): LabTestRepository {
    return LabTestRepository(
        lazy(mode = LazyThreadSafetyMode.NONE) { createSecurePreferences(context) },
        NearbyExposureNotificationApi(
            context,
            Nearby.getExposureNotificationClient(context)
        ),
        labTestService ?: LabTestService.create(context).also { labTestService = it },
        { UploadDiagnosisKeysJob.schedule(context) }
    )
}

fun createAppLifecycleManager(context: Context): AppLifecycleManager {
    return AppLifecycleManager(
        context,
        context.getSharedPreferences("${BuildConfig.APPLICATION_ID}.config", 0),
        AppUpdateManagerFactory.create(context)
    )
}

private fun createSecurePreferences(context: Context): SharedPreferences {
    return notificationPreferences ?: EncryptedSharedPreferences.create(
        "${BuildConfig.APPLICATION_ID}.notifications",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    ).also { notificationPreferences = it }
}
