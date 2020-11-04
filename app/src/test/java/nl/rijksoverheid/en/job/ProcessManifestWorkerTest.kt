/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.job

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.api.CacheStrategy
import nl.rijksoverheid.en.api.CdnService
import nl.rijksoverheid.en.api.model.AppConfig
import nl.rijksoverheid.en.api.model.Manifest
import nl.rijksoverheid.en.api.model.ResourceBundle
import nl.rijksoverheid.en.api.model.RiskCalculationParameters
import nl.rijksoverheid.en.enapi.StatusResult
import nl.rijksoverheid.en.notifier.NotificationsRepository
import nl.rijksoverheid.en.preferences.AsyncSharedPreferences
import nl.rijksoverheid.en.test.FakeExposureNotificationApi
import okhttp3.ResponseBody
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.O_MR1])
class ProcessManifestWorkerTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `Successful sync cancels sync error notification`() {
        val repository = ExposureNotificationsRepository(
            context,
            object : FakeExposureNotificationApi() {
                override suspend fun getStatus(): StatusResult = StatusResult.Enabled
            },
            object : CdnService {
                override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                    throw NotImplementedError()
                }

                override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest =
                    Manifest(listOf(), "risk", "config")

                override suspend fun getRiskCalculationParameters(id: String): RiskCalculationParameters =
                    throw NotImplementedError()

                override suspend fun getAppConfig(
                    id: String,
                    cacheStrategy: CacheStrategy?
                ): AppConfig =
                    AppConfig()

                override suspend fun getResourceBundle(
                    id: String,
                    cacheStrategy: CacheStrategy?
                ): ResourceBundle {
                    throw java.lang.IllegalStateException()
                }
            },
            AsyncSharedPreferences { context.getSharedPreferences("test_repository", 0) },
            object : BackgroundWorkScheduler {
                override fun schedule(intervalMinutes: Int) {
                }

                override fun cancel() {
                }
            },
            mock(),
            mock(),
            mock()
        )

        val notificationManager = ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationsRepository = NotificationsRepository(context)
        notificationsRepository.showSyncIssuesNotification()

        assertTrue(Shadows.shadowOf(notificationManager).activeNotifications.isNotEmpty())

        val worker =
            TestListenableWorkerBuilder<ProcessManifestWorker>(context).setWorkerFactory(object :
                    WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters
                    ): ListenableWorker? {
                        return ProcessManifestWorker(
                            appContext,
                            workerParameters,
                            repository,
                            NotificationsRepository(context)
                        )
                    }
                }).build() as CoroutineWorker

        runBlocking {
            worker.doWork()
        }

        assertTrue(Shadows.shadowOf(notificationManager).activeNotifications.isEmpty())
    }
}
