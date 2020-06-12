/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import android.app.Application
import android.os.Build
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.en.api.ExposureNotificationService
import nl.rijksoverheid.en.api.model.Manifest
import nl.rijksoverheid.en.enapi.DiagnosisKeysResult
import nl.rijksoverheid.en.job.ProcessManifestWorkerScheduler
import nl.rijksoverheid.en.test.FakeExposureNotificationApi
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.O_MR1])
class ExposureNotificationsRepositoryTest {
    private lateinit var mockWebServer: MockWebServer
    private val fakeScheduler = object : ProcessManifestWorkerScheduler {
        override fun schedule(intervalHours: Int) {
            throw NotImplementedError()
        }

        override fun cancel() {
            throw NotImplementedError()
        }
    }

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `processExposureKeySets processes exposure key sets`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("dummy_key_file"))
        mockWebServer.enqueue(
            MockResponse().setBody(
                """
                        {
              "MinimumRiskScore": 1,
              "AttenuationScores": [
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8
              ],
              "DaysSinceLastExposureScores": [
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8
              ],
              "DurationScores": [
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8
              ],
              "TransmissionRiskScores": [
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8
              ],
              "DurationAtAttenuationThresholds": [
                42,
                56
              ]
            }
            """.trimIndent()
            )
        )
        mockWebServer.start()
        val context = ApplicationProvider.getApplicationContext<Application>()
        val service = ExposureNotificationService.create(
            context,
            OkHttpClient(),
            mockWebServer.url("/").toString()
        )
        val config = AtomicReference<ExposureConfiguration>()

        val api = object : FakeExposureNotificationApi() {
            override suspend fun provideDiagnosisKeys(
                files: List<File>,
                configuration: ExposureConfiguration,
                token: String
            ): DiagnosisKeysResult {
                if (files.size != 1) throw AssertionError("Expected one file")
                config.set(configuration)
                return DiagnosisKeysResult.Success
            }
        }

        val sharedPrefs = ApplicationProvider.getApplicationContext<Application>()
            .getSharedPreferences("repository_test", 0)
        val repository = ExposureNotificationsRepository(
            ApplicationProvider.getApplicationContext(), api, service, sharedPrefs, fakeScheduler
        )

        val result =
            repository.processExposureKeySets(Manifest(listOf("test"), "", "config-params"))

        assertEquals(
            ExposureConfiguration.ExposureConfigurationBuilder()
                .setMinimumRiskScore(1)
                .setAttenuationScores(1, 2, 3, 4, 5, 6, 7, 8)
                .setDaysSinceLastExposureScores(1, 2, 3, 4, 5, 6, 7, 8)
                .setDurationScores(1, 2, 3, 4, 5, 6, 7, 8)
                .setTransmissionRiskScores(1, 2, 3, 4, 5, 6, 7, 8)
                .setDurationAtAttenuationThresholds(42, 56).build().toString().trim(),
            config.get().toString().trim()
        )
        assertEquals(2, mockWebServer.requestCount)
        assertEquals(ProcessExposureKeysResult.Success, result)
        assertEquals("/v1/exposurekeyset/test", mockWebServer.takeRequest().path)
        assertEquals(setOf("test"), sharedPrefs.getStringSet("exposure_key_sets", emptySet()))
    }

    @Test
    fun `processExposureKeySets processes only new exposure key sets`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("dummy_key_file"))
        mockWebServer.enqueue(
            MockResponse().setBody(
                """
                        {
              "MinimumRiskScore": 1,
              "AttenuationScores": [
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8
              ],
              "DaysSinceLastExposureScores": [
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8
              ],
              "DurationScores": [
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8
              ],
              "TransmissionRiskScores": [
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8
              ],
              "DurationAtAttenuationThresholds": [
                42,
                56
              ]
            }
            """.trimIndent()
            )
        )
        mockWebServer.start()
        val context = ApplicationProvider.getApplicationContext<Application>()
        val service = ExposureNotificationService.create(
            context,
            OkHttpClient(),
            mockWebServer.url("/").toString()
        )

        val api = object : FakeExposureNotificationApi() {
            override suspend fun provideDiagnosisKeys(
                files: List<File>,
                configuration: ExposureConfiguration,
                token: String
            ): DiagnosisKeysResult {
                if (files.size != 1) throw AssertionError("Expected one file")
                return DiagnosisKeysResult.Success
            }
        }

        val sharedPrefs = ApplicationProvider.getApplicationContext<Application>()
            .getSharedPreferences("repository_test", 0)

        sharedPrefs.edit(commit = true) {
            putStringSet("exposure_key_sets", setOf("test"))
        }

        val repository = ExposureNotificationsRepository(
            ApplicationProvider.getApplicationContext(), api, service, sharedPrefs, fakeScheduler
        )

        val result = repository.processExposureKeySets(
            Manifest(
                listOf("test", "test2"),
                "",
                "config-params"
            )
        )

        assertEquals(2, mockWebServer.requestCount)
        assertEquals(ProcessExposureKeysResult.Success, result)

        assertEquals("/v1/exposurekeyset/test2", mockWebServer.takeRequest().path)
        assertEquals(
            "/v1/riskcalculationparameters/config-params",
            mockWebServer.takeRequest().path
        )

        assertEquals(
            setOf("test", "test2"),
            sharedPrefs.getStringSet("exposure_key_sets", emptySet())
        )
    }

    @Test
    fun `processExposureKeySets already processed are not processed again`() = runBlocking {
        mockWebServer.start()
        val context = ApplicationProvider.getApplicationContext<Application>()
        val service = ExposureNotificationService.create(
            context,
            OkHttpClient(),
            mockWebServer.url("/").toString()
        )

        val api = object : FakeExposureNotificationApi() {
            override suspend fun provideDiagnosisKeys(
                files: List<File>,
                configuration: ExposureConfiguration,
                token: String
            ): DiagnosisKeysResult {
                throw java.lang.AssertionError("Should not be processed")
            }
        }

        val sharedPrefs = ApplicationProvider.getApplicationContext<Application>()
            .getSharedPreferences("repository_test", 0)

        sharedPrefs.edit(commit = true) {
            putStringSet("exposure_key_sets", setOf("test"))
        }
        val repository = ExposureNotificationsRepository(
            ApplicationProvider.getApplicationContext(), api, service, sharedPrefs, fakeScheduler
        )

        val result = repository.processExposureKeySets(Manifest(listOf("test"), "", "config-param"))

        assertEquals(ProcessExposureKeysResult.Success, result)
        assertEquals(0, mockWebServer.requestCount)
        assertEquals(setOf("test"), sharedPrefs.getStringSet("exposure_key_sets", emptySet()))
    }

    @Test
    fun `processExposureKeySets should remove exposure key sets that are no longer live`() =
        runBlocking {
            mockWebServer.start()
            val context = ApplicationProvider.getApplicationContext<Application>()
            val service = ExposureNotificationService.create(
                context,
                OkHttpClient(),
                mockWebServer.url("/").toString()
            )

            val api = object : FakeExposureNotificationApi() {
                override suspend fun provideDiagnosisKeys(
                    files: List<File>,
                    configuration: ExposureConfiguration,
                    token: String
                ): DiagnosisKeysResult {
                    throw java.lang.AssertionError("Should not be processed")
                }
            }

            val sharedPrefs = ApplicationProvider.getApplicationContext<Application>()
                .getSharedPreferences("repository_test", 0)

            sharedPrefs.edit(commit = true) {
                putStringSet("exposure_key_sets", setOf("test"))
            }

            val repository = ExposureNotificationsRepository(
                ApplicationProvider.getApplicationContext(),
                api,
                service,
                sharedPrefs,
                fakeScheduler
            )

            val result = repository.processExposureKeySets(Manifest(listOf(), "", "config-params"))

            assertEquals(ProcessExposureKeysResult.Success, result)
            assertEquals(0, mockWebServer.requestCount)
            assertEquals(
                emptySet<String>(),
                sharedPrefs.getStringSet("exposure_key_sets", setOf("not-empty"))
            )
        }

    @Test
    fun `processExposureKeySets failing to download files returns error`() =
        runBlocking {
            mockWebServer.enqueue(MockResponse().setResponseCode(500))
            mockWebServer.start()
            val context = ApplicationProvider.getApplicationContext<Application>()
            val service = ExposureNotificationService.create(
                context,
                OkHttpClient(),
                mockWebServer.url("/").toString()
            )

            val api = object : FakeExposureNotificationApi() {
                override suspend fun provideDiagnosisKeys(
                    files: List<File>,
                    configuration: ExposureConfiguration,
                    token: String
                ): DiagnosisKeysResult {
                    throw java.lang.AssertionError("Should not be processed")
                }
            }

            val sharedPrefs = ApplicationProvider.getApplicationContext<Application>()
                .getSharedPreferences("repository_test", 0)

            val repository = ExposureNotificationsRepository(
                ApplicationProvider.getApplicationContext(),
                api,
                service,
                sharedPrefs,
                fakeScheduler
            )

            val result =
                repository.processExposureKeySets(Manifest(listOf("test"), "", "config-params"))

            assertEquals(ProcessExposureKeysResult.ServerError, result)
            assertEquals(1, mockWebServer.requestCount)
            assertEquals(
                emptySet<String>(),
                sharedPrefs.getStringSet("exposure_key_sets", setOf())
            )
        }

    @Test
    fun `processExposureKeySets failing to download some files processes files and returns error`() =
        runBlocking {
            // use dispatcher since the key files are fetched in parallel
            mockWebServer.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    return when (request.path) {
                        "/v1/exposurekeyset/test" -> {
                            MockResponse().setResponseCode(500)
                        }
                        "/v1/exposurekeyset/test2" -> {
                            MockResponse().setBody("dummy_key_file")
                        }
                        "/v1/riskcalculationparameters/config-params" -> {
                            MockResponse().setBody(
                                """
                                {"MinimumRiskScore":1,"AttenuationScores":[1,2,3,4,5,6,7,8],"DaysSinceLastExposureScores":[1,2,3,4,5,6,7,8],"DurationScores":[1,2,3,4,5,6,7,8],"TransmissionRiskScores":[1,2,3,4,5,6,7,8],"DurationAtAttenuationThresholds":[42,56]}
                                """.trimIndent()
                            )
                        }
                        else -> {
                            MockResponse().setResponseCode(404)
                        }
                    }
                }
            }

            mockWebServer.start()
            val context = ApplicationProvider.getApplicationContext<Application>()
            val service = ExposureNotificationService.create(
                context,
                OkHttpClient(),
                mockWebServer.url("/").toString()
            )

            val processed = AtomicBoolean(false)
            val api = object : FakeExposureNotificationApi() {
                override suspend fun provideDiagnosisKeys(
                    files: List<File>,
                    configuration: ExposureConfiguration,
                    token: String
                ): DiagnosisKeysResult {
                    if (files.size != 1) throw java.lang.AssertionError()
                    processed.set(true)
                    return DiagnosisKeysResult.Success
                }
            }

            val sharedPrefs = ApplicationProvider.getApplicationContext<Application>()
                .getSharedPreferences("repository_test", 0)

            val repository = ExposureNotificationsRepository(
                ApplicationProvider.getApplicationContext(),
                api,
                service,
                sharedPrefs,
                fakeScheduler
            )

            val result = repository.processExposureKeySets(
                Manifest(
                    listOf("test", "test2"),
                    "",
                    "config-params"
                )
            )

            val request1 = mockWebServer.takeRequest()
            val request2 = mockWebServer.takeRequest()
            val requests = listOf(request1, request2).sortedBy { it.path }

            assertEquals("/v1/exposurekeyset/test", requests[0].path)
            assertEquals("/v1/exposurekeyset/test2", requests[1].path)
            assertEquals(
                "/v1/riskcalculationparameters/config-params",
                mockWebServer.takeRequest().path
            )
            assertTrue(processed.get())
            assertEquals(ProcessExposureKeysResult.ServerError, result)
            assertEquals(3, mockWebServer.requestCount)
            assertEquals(
                setOf("test2"),
                sharedPrefs.getStringSet("exposure_key_sets", emptySet())
            )
        }

    @Test
    fun `addExposure adds exposure`() = runBlocking {
        val dateTime = "2020-06-20T10:15:30.00Z"
        val context = ApplicationProvider.getApplicationContext<Application>()
        val service = ExposureNotificationService.create(
            context,
            OkHttpClient(),
            mockWebServer.url("/").toString()
        )

        val api = object : FakeExposureNotificationApi() {
            override suspend fun getSummary(token: String) =
                if (token == "sample-token") {
                    ExposureSummary.ExposureSummaryBuilder().setDaysSinceLastExposure(4).build()
                } else null
        }

        val sharedPrefs = ApplicationProvider.getApplicationContext<Application>()
            .getSharedPreferences("repository_test", 0)

        val repository = ExposureNotificationsRepository(
            ApplicationProvider.getApplicationContext(),
            api,
            service,
            sharedPrefs,
            fakeScheduler,
            Clock.fixed(Instant.parse(dateTime), ZoneId.of("UTC"))
        )

        val result = repository.addExposure("sample-token")

        assertEquals(4, result)
        assertEquals(
            LocalDate.of(2020, 6, 20).minusDays(4),
            repository.getLastExposureDate().filterNotNull().first()
        )
    }

    @Test
    fun `addExposure while newer exposure exists keeps newer exposure`() = runBlocking {
        val dateTime = "2020-06-20T10:15:30.00Z"
        val context = ApplicationProvider.getApplicationContext<Application>()
        val service = ExposureNotificationService.create(
            context,
            OkHttpClient(),
            mockWebServer.url("/").toString()
        )

        val api = object : FakeExposureNotificationApi() {
            override suspend fun getSummary(token: String) =
                if (token == "sample-token-old") {
                    ExposureSummary.ExposureSummaryBuilder().setDaysSinceLastExposure(8).build()
                } else if (token == "sample-token-new") {
                    ExposureSummary.ExposureSummaryBuilder().setDaysSinceLastExposure(4).build()
                } else null
        }

        val sharedPrefs = ApplicationProvider.getApplicationContext<Application>()
            .getSharedPreferences("repository_test", 0)

        val repository = ExposureNotificationsRepository(
            ApplicationProvider.getApplicationContext(),
            api,
            service,
            sharedPrefs,
            fakeScheduler,
            Clock.fixed(Instant.parse(dateTime), ZoneId.of("UTC"))
        )

        val resultNew = repository.addExposure("sample-token-new")
        val resultOld = repository.addExposure("sample-token-old")

        assertEquals(8, resultOld)
        assertEquals(4, resultNew)
        assertEquals(
            LocalDate.of(2020, 6, 20).minusDays(4),
            repository.getLastExposureDate().filterNotNull().first()
        )
    }
}
