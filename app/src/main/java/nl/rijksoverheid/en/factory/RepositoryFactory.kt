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
import androidx.security.crypto.MasterKey
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.nearby.Nearby
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import kotlinx.coroutines.Dispatchers
import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.api.CdnService
import nl.rijksoverheid.en.api.LabTestService
import nl.rijksoverheid.en.applifecycle.AppLifecycleManager
import nl.rijksoverheid.en.beagle.BeagleHelperImpl
import nl.rijksoverheid.en.config.AppConfigManager
import nl.rijksoverheid.en.dashboard.DashboardRepository
import nl.rijksoverheid.en.enapi.nearby.NearbyExposureNotificationApi
import nl.rijksoverheid.en.job.BackgroundWorkScheduler
import nl.rijksoverheid.en.job.CheckConnectionWorker
import nl.rijksoverheid.en.job.DecoyWorker
import nl.rijksoverheid.en.job.ExposureCleanupWorker
import nl.rijksoverheid.en.job.ProcessManifestWorker
import nl.rijksoverheid.en.job.ScheduleDecoyWorker
import nl.rijksoverheid.en.job.UploadDiagnosisKeysJob
import nl.rijksoverheid.en.labtest.LabTestRepository
import nl.rijksoverheid.en.migration.RecoverBackupHelper.recoverSecurePreferencesFromBackupMigration
import nl.rijksoverheid.en.notifier.NotificationsRepository
import nl.rijksoverheid.en.onboarding.GooglePlayServicesUpToDateChecker
import nl.rijksoverheid.en.onboarding.OnboardingRepository
import nl.rijksoverheid.en.preferences.AsyncSharedPreferences
import nl.rijksoverheid.en.resource.ResourceBundleManager
import nl.rijksoverheid.en.settings.Settings
import nl.rijksoverheid.en.settings.SettingsRepository
import nl.rijksoverheid.en.status.StatusCache
import nl.rijksoverheid.en.util.retry

/**
 * Factory for Repository classes
 */
object RepositoryFactory {

    // cached service instance
    private var cdnService: CdnService? = null
    private var labTestService: LabTestService? = null
    private var notificationPreferences: AsyncSharedPreferences? = null
    private var statusCache: StatusCache? = null

    private const val MINIMUM_PLAY_SERVICES_VERSION = 202665000

    private fun createCdnService(context: Context): CdnService {
        return cdnService ?: CdnService.create(context, BuildConfig.VERSION_CODE)
            .also { cdnService = it }
    }

    fun createExposureNotificationsRepository(context: Context): ExposureNotificationsRepository {
        val service = createCdnService(context)
        val statusCache = statusCache ?: StatusCache(
            context.getSharedPreferences("${BuildConfig.APPLICATION_ID}.cache", 0)
        ).also { statusCache = it }

        return ExposureNotificationsRepository(
            context,
            NearbyExposureNotificationApi(
                context,
                Nearby.getExposureNotificationClient(context)
            ),
            service,
            createSecurePreferences(context),
            object : BackgroundWorkScheduler {
                override fun schedule(intervalMinutes: Int) {
                    ProcessManifestWorker.queue(context, intervalMinutes)
                    CheckConnectionWorker.queue(context)
                    ScheduleDecoyWorker.queue(context)
                    ExposureCleanupWorker.queue(context)
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
            AppConfigManager(
                service,
                BeagleHelperImpl.useDebugFeatureFlags,
                BeagleHelperImpl.getDebugFeatureFlags
            )
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
        return labTestService ?: LabTestService.create(context, BuildConfig.VERSION_CODE)
            .also { labTestService = it }
    }

    fun createAppConfigManager(context: Context): AppConfigManager {
        return AppConfigManager(
            createCdnService(context),
            BeagleHelperImpl.useDebugFeatureFlags,
            BeagleHelperImpl.getDebugFeatureFlags
        )
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
            createCdnService(context),
            useDefaultGuidance = BeagleHelperImpl.useDefaultGuidance
        )
    }

    fun createSettingsRepository(context: Context): SettingsRepository {
        return SettingsRepository(context, Settings(context))
    }

    fun createDashboardRepository(context: Context): DashboardRepository {
        return DashboardRepository(createCdnService(context), Dispatchers.IO)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun createSecurePreferences(context: Context): AsyncSharedPreferences {
        fun create(fileName: String): SharedPreferences {
            val masterKey =
                MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            return EncryptedSharedPreferences.create(
                context,
                fileName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        return notificationPreferences ?: AsyncSharedPreferences {
            val fileName = "${BuildConfig.APPLICATION_ID}.notifications"

            try {
                retry { create(fileName) }
            } catch (e: Exception) {
                recoverSecurePreferencesFromBackupMigration(context, fileName)
                create(fileName)
            }
        }.also { notificationPreferences = it }
    }
}
