/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.factory

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.nearby.Nearby
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.announcement.AnnouncementRepository
import nl.rijksoverheid.en.api.CdnService
import nl.rijksoverheid.en.api.LabTestService
import nl.rijksoverheid.en.applifecycle.AppLifecycleManager
import nl.rijksoverheid.en.config.AppConfigManager
import nl.rijksoverheid.en.enapi.NearbyExposureNotificationApi
import nl.rijksoverheid.en.job.BackgroundWorkScheduler
import nl.rijksoverheid.en.job.CheckConnectionWorker
import nl.rijksoverheid.en.job.DecoyWorker
import nl.rijksoverheid.en.job.ProcessManifestWorker
import nl.rijksoverheid.en.job.ScheduleDecoyWorker
import nl.rijksoverheid.en.job.UploadDiagnosisKeysJob
import nl.rijksoverheid.en.labtest.LabTestRepository
import nl.rijksoverheid.en.notifier.NotificationsRepository
import nl.rijksoverheid.en.onboarding.GooglePlayServicesUpToDateChecker
import nl.rijksoverheid.en.onboarding.OnboardingRepository
import nl.rijksoverheid.en.preferences.AsyncSharedPreferences
import nl.rijksoverheid.en.resource.ResourceBundleManager
import nl.rijksoverheid.en.status.StatusCache
import nl.rijksoverheid.en.util.retry

// cached service instance
private var cdnService: CdnService? = null
private var labTestService: LabTestService? = null
private var notificationPreferences: AsyncSharedPreferences? = null
private var statusCache: StatusCache? = null

private const val MINIMUM_PLAY_SERVICES_VERSION = 202665000

fun createExposureNotificationsRepository(context: Context): ExposureNotificationsRepository {
    val service = cdnService ?: CdnService.create(context, BuildConfig.VERSION_CODE).also { cdnService = it }
    val statusCache = statusCache ?: StatusCache(
        context.getSharedPreferences("${BuildConfig.APPLICATION_ID}.cache", 0)
    ).also { statusCache = it }

    return ExposureNotificationsRepository(
        context,
        NearbyExposureNotificationApi(context, Nearby.getExposureNotificationClient(context)),
        service,
        createSecurePreferences(context),
        object : BackgroundWorkScheduler {
            override fun schedule(intervalMinutes: Int) {
                ProcessManifestWorker.queue(context, intervalMinutes)
                CheckConnectionWorker.queue(context)
                ScheduleDecoyWorker.queue(context)
            }

            override fun cancel() {
                ProcessManifestWorker.cancel(context)
                CheckConnectionWorker.cancel(context)
                ScheduleDecoyWorker.cancel(context)
                DecoyWorker.cancel(context)
            }
        },
        createAppLifecycleManager(context),
        statusCache,
        AppConfigManager(service)
    )
}

private fun createGooglePlayServicesChecker(context: Context): GooglePlayServicesUpToDateChecker =
    {
        val result = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context, MINIMUM_PLAY_SERVICES_VERSION)
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

fun createAnnouncementRepository(context: Context): AnnouncementRepository {
    return AnnouncementRepository(
        context.getSharedPreferences("${BuildConfig.APPLICATION_ID}.announcement", 0),
    )
}

fun createLabTestRepository(context: Context): LabTestRepository {
    return LabTestRepository(
        createSecurePreferences(context),
        NearbyExposureNotificationApi(
            context,
            Nearby.getExposureNotificationClient(context)
        ),
        createLabTestService(context),
        { UploadDiagnosisKeysJob.schedule(context) },
        { delayMillis -> DecoyWorker.queue(context, delayMillis) },
        createAppConfigManager(context)
    )
}

private fun createLabTestService(context: Context): LabTestService {
    return labTestService ?: LabTestService.create(context, BuildConfig.VERSION_CODE).also { labTestService = it }
}

fun createAppConfigManager(context: Context): AppConfigManager {
    val service = cdnService ?: CdnService.create(context, BuildConfig.VERSION_CODE).also { cdnService = it }
    return AppConfigManager(service)
}

fun createAppLifecycleManager(context: Context): AppLifecycleManager {
    return AppLifecycleManager(
        context,
        context.getSharedPreferences("${BuildConfig.APPLICATION_ID}.config", 0),
        AppUpdateManagerFactory.create(context)
    ) { NotificationsRepository(context).showAppUpdateNotification() }
}

fun createResourceBundleManager(context: Context): ResourceBundleManager {
    return ResourceBundleManager(
        context,
        cdnService ?: CdnService.create(context, BuildConfig.VERSION_CODE).also { cdnService = it }
    )
}

@Suppress("BlockingMethodInNonBlockingContext")
private fun createSecurePreferences(context: Context): AsyncSharedPreferences {
    return notificationPreferences ?: AsyncSharedPreferences {
        retry {
            EncryptedSharedPreferences.create(
                "${BuildConfig.APPLICATION_ID}.notifications",
                MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }.also { notificationPreferences = it }
}
