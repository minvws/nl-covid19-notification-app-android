/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import android.app.Application
import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.LocationManager
import android.os.Build
import androidx.core.content.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.yield
import nl.rijksoverheid.en.api.CdnService
import nl.rijksoverheid.en.api.model.AppConfig
import nl.rijksoverheid.en.api.model.Manifest
import nl.rijksoverheid.en.api.model.RiskCalculationParameters
import nl.rijksoverheid.en.applifecycle.AppLifecycleManager
import nl.rijksoverheid.en.config.AppConfigManager
import nl.rijksoverheid.en.enapi.DiagnosisKeysResult
import nl.rijksoverheid.en.enapi.DisableNotificationsResult
import nl.rijksoverheid.en.enapi.EnableNotificationsResult
import nl.rijksoverheid.en.enapi.StatusResult
import nl.rijksoverheid.en.enapi.nearby.ExposureNotificationApi
import nl.rijksoverheid.en.job.ProcessManifestWorkerScheduler
import nl.rijksoverheid.en.notifier.NotificationsRepository
import nl.rijksoverheid.en.status.StatusCache
import nl.rijksoverheid.en.test.FakeExposureNotificationApi
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import retrofit2.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private val MOCK_RISK_PARAMS_RESPONSE = MockResponse().setBody(
    """
                        {
              "minimumRiskScore": 1,
              "attenuationScores": [
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8
              ],
              "daysSinceLastExposureScores": [
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8
              ],
              "durationScores": [
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8
              ],
              "transmissionRiskScores": [
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8
              ],
              "durationAtAttenuationThresholds": [
                42,
                56
              ]
            }
            """.trimIndent()
)

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.O_MR1])
class ExposureNotificationsRepositoryTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var context: Context

    private val fakeScheduler = object : ProcessManifestWorkerScheduler {
        override fun schedule(intervalMinutes: Int) {
            throw NotImplementedError()
        }

        override fun cancel() {
            throw NotImplementedError()
        }
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockWebServer = MockWebServer()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `processExposureKeySets processes exposure key sets`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("dummy_key_file"))
        mockWebServer.enqueue(MOCK_RISK_PARAMS_RESPONSE)
        mockWebServer.start()
        val context = ApplicationProvider.getApplicationContext<Application>()
        val service = CdnService.create(
            context,
            OkHttpClient(),
            mockWebServer.url("/").toString()
        )
        val config = AtomicReference<ExposureConfiguration>()

        val api = object : FakeExposureNotificationApi() {
            override suspend fun getStatus(): StatusResult = StatusResult.Enabled
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

        val repository =
            createRepository(api = api, cdnService = service, preferences = sharedPrefs)

        val result =
            repository.processExposureKeySets(
                Manifest(
                    listOf("test"),
                    "",
                    "config-params",
                    "appConfigId"
                )
            )

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
        assertEquals("/v01/exposurekeyset/test", mockWebServer.takeRequest().path)
        assertEquals(setOf("test"), sharedPrefs.getStringSet("exposure_key_sets", emptySet()))
    }

    @Test
    fun `processExposureKeySets processes only new exposure key sets`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("dummy_key_file"))
        mockWebServer.enqueue(MOCK_RISK_PARAMS_RESPONSE)
        mockWebServer.start()
        val context = ApplicationProvider.getApplicationContext<Application>()
        val service = CdnService.create(
            context,
            OkHttpClient(),
            mockWebServer.url("/").toString()
        )

        val api = object : FakeExposureNotificationApi() {
            override suspend fun getStatus(): StatusResult = StatusResult.Enabled

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

        val repository =
            createRepository(api = api, cdnService = service, preferences = sharedPrefs)

        val result = repository.processExposureKeySets(
            Manifest(
                listOf("test", "test2"),
                "",
                "config-params",
                "appConfigId"
            )
        )

        assertEquals(2, mockWebServer.requestCount)
        assertEquals(ProcessExposureKeysResult.Success, result)

        assertEquals("/v01/exposurekeyset/test2", mockWebServer.takeRequest().path)
        assertEquals(
            "/v01/riskcalculationparameters/config-params",
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
        val service = CdnService.create(
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

        val repository =
            createRepository(api = api, cdnService = service, preferences = sharedPrefs)

        val result = repository.processExposureKeySets(
            Manifest(
                listOf("test"),
                "",
                "config-param",
                "appConfigId"
            )
        )

        assertEquals(ProcessExposureKeysResult.Success, result)
        assertEquals(0, mockWebServer.requestCount)
        assertEquals(setOf("test"), sharedPrefs.getStringSet("exposure_key_sets", emptySet()))
    }

    @Test
    fun `processExposureKeySets should remove exposure key sets that are no longer live`() =
        runBlocking {
            mockWebServer.start()
            val context = ApplicationProvider.getApplicationContext<Application>()
            val service = CdnService.create(
                context,
                OkHttpClient(),
                mockWebServer.url("/").toString()
            )

            val api = object : FakeExposureNotificationApi() {
                override suspend fun getStatus(): StatusResult = StatusResult.Enabled

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

            val repository =
                createRepository(api = api, cdnService = service, preferences = sharedPrefs)

            val result = repository.processExposureKeySets(
                Manifest(
                    listOf(),
                    "",
                    "config-params",
                    "appConfigId"
                )
            )

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
            val service = CdnService.create(
                context,
                OkHttpClient(),
                mockWebServer.url("/").toString()
            )

            val api = object : FakeExposureNotificationApi() {
                override suspend fun getStatus(): StatusResult = StatusResult.Enabled
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

            val repository =
                createRepository(api = api, cdnService = service, preferences = sharedPrefs)

            val result =
                repository.processExposureKeySets(
                    Manifest(
                        listOf("test"),
                        "",
                        "config-params",
                        "appConfigId"
                    )
                )

            assertEquals(ProcessExposureKeysResult.ServerError, result)
            assertEquals(1, mockWebServer.requestCount)
            assertEquals(
                emptySet<String>(),
                sharedPrefs.getStringSet("exposure_key_sets", setOf())
            )
        }

    @Test
    fun `processExposureKeySets file without signature marked as processed and skipped`() =
        runBlocking {
            mockWebServer.enqueue(
                MockResponse().setBody(
                    Buffer().readFrom(
                        ExposureNotificationsRepositoryTest::class.java.getResourceAsStream("/export-no-sig.zip")!!
                    )
                )
            )
            mockWebServer.start()
            val context = ApplicationProvider.getApplicationContext<Application>()
            val service = CdnService.create(
                context,
                OkHttpClient(),
                mockWebServer.url("/").toString()
            )

            val api = object : FakeExposureNotificationApi() {
                override suspend fun getStatus(): StatusResult = StatusResult.Enabled

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

            val repository = createRepository(
                api = api,
                cdnService = service,
                preferences = sharedPrefs,
                signatureValidation = true
            )

            val result =
                repository.processExposureKeySets(
                    Manifest(
                        listOf("test"),
                        "",
                        "config-params",
                        "appConfigId"
                    )
                )

            assertEquals(ProcessExposureKeysResult.Success, result)
            assertEquals(1, mockWebServer.requestCount)
            assertEquals(
                setOf("test"),
                sharedPrefs.getStringSet("exposure_key_sets", emptySet())
            )
        }

    @Test
    fun `processExposureKeySets file with incorrect signature marked as processed and skipped`() =
        runBlocking {
            mockWebServer.enqueue(
                MockResponse().setBody(
                    Buffer().readFrom(
                        ExposureNotificationsRepositoryTest::class.java.getResourceAsStream("/export-incorrect-sig.zip")!!
                    )
                )
            )
            mockWebServer.start()
            val context = ApplicationProvider.getApplicationContext<Application>()
            val service = CdnService.create(
                context,
                OkHttpClient(),
                mockWebServer.url("/").toString()
            )

            val api = object : FakeExposureNotificationApi() {
                override suspend fun getStatus(): StatusResult = StatusResult.Enabled
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

            val repository =
                createRepository(api = api, cdnService = service, signatureValidation = true)

            val result =
                repository.processExposureKeySets(
                    Manifest(
                        listOf("test"),
                        "",
                        "config-params",
                        "appConfigId"
                    )
                )

            assertEquals(ProcessExposureKeysResult.Success, result)
            assertEquals(1, mockWebServer.requestCount)
            assertEquals(
                setOf("test"),
                sharedPrefs.getStringSet("exposure_key_sets", emptySet())
            )
        }

    @Test
    fun `processExposureKeySets failing to download some files processes files and returns error`() =
        runBlocking {
            // use dispatcher since the key files are fetched in parallel
            mockWebServer.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    return when (request.path) {
                        "/v01/exposurekeyset/test" -> {
                            MockResponse().setResponseCode(500)
                        }
                        "/v01/exposurekeyset/test2" -> {
                            MockResponse().setBody("dummy_key_file")
                        }
                        "/v01/riskcalculationparameters/config-params" -> MOCK_RISK_PARAMS_RESPONSE

                        else -> {
                            MockResponse().setResponseCode(404)
                        }
                    }
                }
            }

            mockWebServer.start()
            val context = ApplicationProvider.getApplicationContext<Application>()
            val service = CdnService.create(
                context,
                OkHttpClient(),
                mockWebServer.url("/").toString()
            )

            val processed = AtomicBoolean(false)
            val api = object : FakeExposureNotificationApi() {
                override suspend fun getStatus(): StatusResult = StatusResult.Enabled
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

            val repository =
                createRepository(api = api, cdnService = service, preferences = sharedPrefs)

            val result = repository.processExposureKeySets(
                Manifest(
                    listOf("test", "test2"),
                    "",
                    "config-params",
                    "appConfigId"
                )
            )

            val request1 = mockWebServer.takeRequest()
            val request2 = mockWebServer.takeRequest()
            val requests = listOf(request1, request2).sortedBy { it.path }

            assertEquals("/v01/exposurekeyset/test", requests[0].path)
            assertEquals("/v01/exposurekeyset/test2", requests[1].path)
            assertEquals(
                "/v01/riskcalculationparameters/config-params",
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
    fun `addExposure adds exposure and returns Notify`() = runBlocking {
        val dateTime = "2020-06-20T10:15:30.00Z"

        val api = object : FakeExposureNotificationApi() {
            override suspend fun getSummary(token: String) =
                if (token == "sample-token") {
                    ExposureSummary.ExposureSummaryBuilder().setDaysSinceLastExposure(4)
                        .setMatchedKeyCount(1).build()
                } else null
        }

        val repository = createRepository(
            api = api,
            clock = Clock.fixed(Instant.parse(dateTime), ZoneId.of("UTC"))
        )

        val result = repository.addExposure("sample-token")

        assertTrue(result is AddExposureResult.Notify)
        assertEquals(
            LocalDate.of(2020, 6, 20).minusDays(4),
            repository.getLastExposureDate().filterNotNull().first()
        )
    }

    @Test
    fun `addExposure while newer exposure exists keeps newer exposure`() = runBlocking {
        val dateTime = "2020-06-20T10:15:30.00Z"

        val api = object : FakeExposureNotificationApi() {
            override suspend fun getSummary(token: String) =
                when (token) {
                    "sample-token-old" -> ExposureSummary.ExposureSummaryBuilder()
                        .setDaysSinceLastExposure(8).setMatchedKeyCount(1).build()
                    "sample-token-new" -> ExposureSummary.ExposureSummaryBuilder()
                        .setDaysSinceLastExposure(4).setMatchedKeyCount(1).build()
                    else -> null
                }
        }

        val repository = createRepository(
            api = api,
            clock = Clock.fixed(Instant.parse(dateTime), ZoneId.of("UTC"))
        )

        repository.addExposure("sample-token-new")
        repository.addExposure("sample-token-old")

        assertEquals(
            LocalDate.of(2020, 6, 20).minusDays(4),
            repository.getLastExposureDate().filterNotNull().first()
        )
    }

    @Test
    fun `addExposure without matching keys is ignored`() = runBlocking {
        val dateTime = "2020-06-20T10:15:30.00Z"

        val api = object : FakeExposureNotificationApi() {
            override suspend fun getSummary(token: String) =
                ExposureSummary.ExposureSummaryBuilder().setMatchedKeyCount(0).build()
        }

        val repository = createRepository(
            api = api,
            clock = Clock.fixed(Instant.parse(dateTime), ZoneId.of("UTC"))
        )

        repository.addExposure("sample-token-new")

        assertEquals(null, repository.getLastExposureDate().first())
    }

    @Test
    fun `addExposure with max risk score below threshold is ignored`() = runBlocking {
        val dateTime = "2020-06-20T10:15:30.00Z"

        val api = object : FakeExposureNotificationApi() {
            override suspend fun getSummary(token: String) =
                ExposureSummary.ExposureSummaryBuilder().setMatchedKeyCount(1)
                    .setMaximumRiskScore(1).build()
        }

        val sharedPrefs = ApplicationProvider.getApplicationContext<Application>()
            .getSharedPreferences("repository_test", 0)

        sharedPrefs.edit {
            putInt("min_risk_score", 10)
        }

        val repository = createRepository(
            api = api,
            preferences = sharedPrefs,
            clock = Clock.fixed(Instant.parse(dateTime), ZoneId.of("UTC"))
        )

        repository.addExposure("sample-token-new")

        assertEquals(null, repository.getLastExposureDate().first())
    }

    @Test
    fun `processManifest marks the timestamp of last successful time the keys have been processed and returns Success`() =
        runBlocking {
            val dateTime = "2020-06-20T10:15:30.00Z"
            val fakeService = object : CdnService {
                override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                    throw NotImplementedError()
                }

                override suspend fun getManifest(cacheHeader: String?): Manifest =
                    Manifest(emptyList(), "dummy", "riskParamId", "configId")

                override suspend fun getRiskCalculationParameters(id: String): RiskCalculationParameters {
                    throw NotImplementedError()
                }

                override suspend fun getAppConfig(id: String, cacheHeader: String?): AppConfig =
                    AppConfig(1, 5, 0.0)
            }

            val context = ApplicationProvider.getApplicationContext<Application>()
            val sharedPrefs = context.getSharedPreferences("repository_test", 0)

            val repository = createRepository(
                api = object : FakeExposureNotificationApi() {
                    override suspend fun getStatus(): StatusResult = StatusResult.Enabled
                },
                cdnService = fakeService,
                preferences = sharedPrefs,
                clock = Clock.fixed(Instant.parse(dateTime), ZoneId.of("UTC"))
            )

            val result = repository.processManifest()

            assertTrue(result is ProcessManifestResult.Success)
            assertEquals(5, (result as ProcessManifestResult.Success).nextIntervalMinutes)
            assertEquals(
                Instant.parse(dateTime),
                Instant.ofEpochMilli(sharedPrefs.getLong("last_keys_processed", 0))
            )
        }

    @Test
    fun `processManifest stops processing when the app is disabled`() =
        runBlocking {
            val dateTime = "2020-06-20T10:15:30.00Z"
            val fakeService = object : CdnService {
                override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                    throw NotImplementedError()
                }

                override suspend fun getManifest(cacheHeader: String?): Manifest =
                    Manifest(emptyList(), "dummy", "riskParamId", "configId")

                override suspend fun getRiskCalculationParameters(id: String): RiskCalculationParameters {
                    throw NotImplementedError()
                }

                override suspend fun getAppConfig(id: String, cacheHeader: String?): AppConfig =
                    AppConfig(1, 5, 0.0, deactivated = true)
            }

            val context = ApplicationProvider.getApplicationContext<Application>()
            val sharedPrefs = context.getSharedPreferences("repository_test", 0)
            val enDisabled = AtomicBoolean(false)
            val jobsCancelled = AtomicBoolean(false)

            val repository = createRepository(
                api = object : FakeExposureNotificationApi() {
                    override suspend fun getStatus(): StatusResult = StatusResult.Enabled
                    override suspend fun disableNotifications(): DisableNotificationsResult {
                        enDisabled.set(true)
                        return DisableNotificationsResult.Disabled
                    }
                },
                scheduler = object : ProcessManifestWorkerScheduler {
                    override fun schedule(intervalMinutes: Int) {
                    }

                    override fun cancel() {
                        jobsCancelled.set(true)
                    }
                },
                cdnService = fakeService,
                preferences = sharedPrefs,
                clock = Clock.fixed(Instant.parse(dateTime), ZoneId.of("UTC"))
            )

            val result = repository.processManifest()

            assertTrue(result is ProcessManifestResult.Disabled)
            assertTrue(jobsCancelled.get())
            assertTrue(enDisabled.get())
        }

    @Test
    fun `processManifest does not update the timestamp of last successful time if manifest cannot be fetched and returns Error`() =
        runBlocking {
            val dateTime = "2020-06-20T10:15:30.00Z"
            val dateTimeSuccess = "2020-06-20T01:15:30.00Z"

            mockWebServer.enqueue(MockResponse().setResponseCode(500))
            mockWebServer.start()

            val fakeService = CdnService.create(
                ApplicationProvider.getApplicationContext<Application>(),
                OkHttpClient(),
                mockWebServer.url("/").toString()
            )

            val context = ApplicationProvider.getApplicationContext<Application>()
            val sharedPrefs = context.getSharedPreferences("repository_test", 0)

            sharedPrefs.edit {
                putLong("last_keys_processed", Instant.parse(dateTimeSuccess).toEpochMilli())
            }

            val repository = createRepository(
                api = object : FakeExposureNotificationApi() {
                    override suspend fun getStatus(): StatusResult = StatusResult.Enabled
                },
                cdnService = fakeService,
                preferences = sharedPrefs,
                clock = Clock.fixed(Instant.parse(dateTime), ZoneId.of("UTC"))
            )

            val result = repository.processManifest()

            assertTrue(result is ProcessManifestResult.Error)
            assertEquals(1, mockWebServer.requestCount)
            assertEquals(
                Instant.parse(dateTimeSuccess),
                Instant.ofEpochMilli(sharedPrefs.getLong("last_keys_processed", 0))
            )
        }

    @Test
    fun `processManifest shows a notification when the app version is outdated and returns Success`() =
        runBlocking {
            val dateTime = "2020-06-20T10:15:30.00Z"
            val fakeService = object : CdnService {
                override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                    throw NotImplementedError()
                }

                override suspend fun getManifest(cacheHeader: String?): Manifest =
                    Manifest(emptyList(), "dummy", "riskParamId", "configId")

                override suspend fun getRiskCalculationParameters(id: String): RiskCalculationParameters {
                    throw NotImplementedError()
                }

                override suspend fun getAppConfig(id: String, cacheHeader: String?): AppConfig =
                    AppConfig(BuildConfig.VERSION_CODE + 1, 5, 0.0)
            }

            val context = ApplicationProvider.getApplicationContext<Application>()

            val repository = createRepository(
                context,
                api = object : FakeExposureNotificationApi() {
                    override suspend fun getStatus(): StatusResult = StatusResult.Enabled
                },
                appLifecycleManager = AppLifecycleManager(
                    context.getSharedPreferences(
                        "test_config",
                        0
                    ), mock()
                ) {
                    NotificationsRepository(context).showAppUpdateNotification()
                },
                cdnService = fakeService,
                clock = Clock.fixed(Instant.parse(dateTime), ZoneId.of("UTC"))
            )

            val result = repository.processManifest()

            val notificationService =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val shadowNotificationManager = shadowOf(notificationService)

            assertEquals(1, shadowNotificationManager.size())
            assertTrue(result is ProcessManifestResult.Success)
        }

    @Test
    fun `processManifest shows no notification when the app version is not outdated and returns Success`() =
        runBlocking {
            val dateTime = "2020-06-20T10:15:30.00Z"
            val fakeService = object : CdnService {
                override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                    throw NotImplementedError()
                }

                override suspend fun getManifest(cacheHeader: String?): Manifest =
                    Manifest(emptyList(), "dummy", "riskParamId", "configId")

                override suspend fun getRiskCalculationParameters(id: String): RiskCalculationParameters {
                    throw NotImplementedError()
                }

                override suspend fun getAppConfig(id: String, cacheHeader: String?): AppConfig =
                    AppConfig(0, 5, 0.0)
            }

            val context = ApplicationProvider.getApplicationContext<Application>()
            val sharedPrefs = context.getSharedPreferences("repository_test", 0)

            val repository = createRepository(
                context,
                api = object : FakeExposureNotificationApi() {
                    override suspend fun getStatus(): StatusResult = StatusResult.Enabled
                },
                cdnService = fakeService,
                preferences = sharedPrefs,
                clock = Clock.fixed(Instant.parse(dateTime), ZoneId.of("UTC"))
            )

            val result = repository.processManifest()

            val notificationService =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val shadowNotificationManager = shadowOf(notificationService)

            assertEquals(0, shadowNotificationManager.size())
            assertTrue(result is ProcessManifestResult.Success)
        }

    @Test
    fun `processManifest with disabled exposure notifications does not cancel jobs`() =
        runBlocking {
            val dateTime = "2020-06-20T10:15:30.00Z"

            val context = ApplicationProvider.getApplicationContext<Application>()
            val sharedPrefs = context.getSharedPreferences("repository_test", 0)

            val cancelled = AtomicBoolean(false)

            val repository = createRepository(
                api = object : FakeExposureNotificationApi() {
                    override suspend fun getStatus(): StatusResult = StatusResult.Disabled
                    override suspend fun disableNotifications(): DisableNotificationsResult =
                        DisableNotificationsResult.Disabled
                },
                cdnService = object : CdnService {
                    override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                        throw NotImplementedError()
                    }

                    override suspend fun getManifest(cacheHeader: String?): Manifest = Manifest(
                        listOf(), "res", "risk", "config"
                    )

                    override suspend fun getRiskCalculationParameters(id: String): RiskCalculationParameters {
                        throw NotImplementedError()
                    }

                    override suspend fun getAppConfig(id: String, cacheHeader: String?): AppConfig =
                        AppConfig()
                },
                scheduler = object : ProcessManifestWorkerScheduler {
                    override fun schedule(intervalMinutes: Int) {
                        throw IllegalStateException()
                    }

                    override fun cancel() {
                        cancelled.set(true)
                    }
                },
                preferences = sharedPrefs,
                clock = Clock.fixed(Instant.parse(dateTime), ZoneId.of("UTC"))
            )

            val result = repository.processManifest()
            assertTrue(result is ProcessManifestResult.Success)
            assertFalse(cancelled.get())
        }

    @Test
    fun `getLastExposureDate returns date added through addExposure and cancels notification`() =
        runBlocking {
            val dateTime = "2020-06-20T10:15:30.00Z"
            val clock = Clock.fixed(Instant.parse(dateTime), ZoneId.of("UTC"))

            val sharedPrefs = ApplicationProvider.getApplicationContext<Application>()
                .getSharedPreferences("repository_test", 0)

            val api = object : FakeExposureNotificationApi() {
                override suspend fun getSummary(token: String) =
                    if (token == "sample-token") ExposureSummary.ExposureSummaryBuilder()
                        .setDaysSinceLastExposure(4)
                        .setMatchedKeyCount(1).build()
                    else null
            }

            val repository = createRepository(api = api, preferences = sharedPrefs, clock = clock)
            repository.addExposure("sample-token")

            val result = repository.getLastExposureDate().first()
            assertEquals(LocalDate.now(clock).minusDays(4), result)
        }

    @Test
    fun `keyProcessingOverdue returns true if last successful time of key processing is more than 24 hours in the past`() {
        val lastSyncDateTime = "2020-06-20T10:15:30.00Z"
        val dateTime = "2020-06-21T10:16:30.00Z"
        val fakeService = object : CdnService {
            override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                throw NotImplementedError()
            }

            override suspend fun getManifest(cacheHeader: String?): Manifest {
                throw NotImplementedError()
            }

            override suspend fun getRiskCalculationParameters(id: String): RiskCalculationParameters {
                throw NotImplementedError()
            }

            override suspend fun getAppConfig(id: String, cacheHeader: String?): AppConfig =
                AppConfig(1, 5, 0.0)
        }

        val context = ApplicationProvider.getApplicationContext<Application>()
        val sharedPrefs = context.getSharedPreferences("repository_test", 0)

        sharedPrefs.edit {
            putLong("last_keys_processed", Instant.parse(lastSyncDateTime).toEpochMilli())
        }

        val repository = createRepository(
            context = context,
            preferences = sharedPrefs,
            cdnService = fakeService,
            clock = Clock.fixed(Instant.parse(dateTime), ZoneId.of("UTC"))
        )

        assertTrue(repository.keyProcessingOverdue)
    }

    @Test
    fun `keyProcessingOverdue returns false if no timestamp is stored`() {
        val dateTime = "2020-06-21T10:15:30.00Z"
        val fakeService = object : CdnService {
            override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                throw NotImplementedError()
            }

            override suspend fun getManifest(cacheHeader: String?): Manifest {
                throw NotImplementedError()
            }

            override suspend fun getRiskCalculationParameters(id: String): RiskCalculationParameters {
                throw NotImplementedError()
            }

            override suspend fun getAppConfig(id: String, cacheHeader: String?): AppConfig =
                AppConfig(1, 5, 0.0)
        }

        val repository = createRepository(
            cdnService = fakeService,
            clock = Clock.fixed(Instant.parse(dateTime), ZoneId.of("UTC"))
        )

        assertFalse(repository.keyProcessingOverdue)
    }

    @Test
    fun `validateAndWrite only keeps export files from exposure set zip file`() {
        val input = File.createTempFile("test", ".zip")
        ZipOutputStream(FileOutputStream(input)).use {
            it.putNextEntry(ZipEntry("export.bin"))
            it.write(1)
            it.closeEntry()

            it.putNextEntry(ZipEntry("export.sig"))
            it.write(1)
            it.closeEntry()

            it.putNextEntry(ZipEntry("content.sig"))
            it.write(1)
            it.closeEntry()

            it.putNextEntry(ZipEntry("random.junk"))
            it.write(1)
            it.closeEntry()
            it.finish()
        }

        val output = File.createTempFile("test", "zip")

        val repository = createRepository(clock = Clock.systemUTC())

        ZipInputStream(FileInputStream(input)).use {
            repository.validateAndWrite("id", it, output)
        }

        val entries = mutableListOf<String>()
        ZipInputStream(FileInputStream(output)).use {
            do {
                val entry = it.nextEntry ?: break
                entries.add(entry.name)
                it.closeEntry()
            } while (true)
        }

        entries.sort()

        assertEquals(listOf("export.bin", "export.sig"), entries)
    }

    @Test
    fun `getStatus emits cached status and then up-to-date status`() = runBlocking {
        val api = object : FakeExposureNotificationApi() {
            override suspend fun getStatus(): StatusResult {
                return StatusResult.Enabled
            }
        }
        val context = ApplicationProvider.getApplicationContext<Application>()
        val sharedPrefs = context.getSharedPreferences("repository_test", 0)
        val statusCache = StatusCache(sharedPrefs).apply {
            updateCachedStatus(StatusCache.CachedStatus.DISABLED)
        }
        (context.getSystemService(BluetoothManager::class.java) as BluetoothManager).adapter.enable()
        shadowOf(context.getSystemService(LocationManager::class.java) as LocationManager).apply {
            setLocationEnabled(true)
            setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        }

        val repository = createRepository(
            context = context,
            api = api,
            preferences = sharedPrefs,
            statusCache = statusCache
        )

        val result = mutableListOf<StatusResult>()
        repository.getStatus().take(2).toList(result)

        assertEquals(listOf(StatusResult.Disabled, StatusResult.Enabled), result)
    }

    @Test
    fun `getStatus emits when cache changes`() = runBlockingTest {
        val api = object : FakeExposureNotificationApi() {
            override suspend fun getStatus() = StatusResult.Enabled
        }
        val context = ApplicationProvider.getApplicationContext<Application>()
        val sharedPrefs = context.getSharedPreferences("repository_test", 0)
        val statusCache = StatusCache(sharedPrefs).apply {
            updateCachedStatus(StatusCache.CachedStatus.ENABLED)
        }

        val repository = createRepository(
            context = context,
            api = api,
            preferences = sharedPrefs,
            statusCache = statusCache
        )

        (context.getSystemService(BluetoothManager::class.java) as BluetoothManager).adapter.enable()
        shadowOf(context.getSystemService(LocationManager::class.java) as LocationManager).apply {
            setLocationEnabled(true)
            setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        }

        val result = async { repository.getStatus().take(3).toList() }
        yield()

        statusCache.updateCachedStatus(StatusCache.CachedStatus.DISABLED)

        assertEquals(
            listOf(
                StatusResult.Enabled,
                StatusResult.Disabled,
                StatusResult.Enabled
            ), result.await()
        )
    }

    @Test
    fun `getStatus re-triggers when location state changes change`() = runBlockingTest {
        val api = object : FakeExposureNotificationApi() {
            override suspend fun getStatus() = StatusResult.Enabled
        }
        val context = ApplicationProvider.getApplicationContext<Application>()
        val sharedPrefs = context.getSharedPreferences("repository_test", 0)
        val statusCache = StatusCache(sharedPrefs).apply {
            updateCachedStatus(StatusCache.CachedStatus.ENABLED)
        }

        val repository = createRepository(
            context = context,
            api = api,
            preferences = sharedPrefs,
            statusCache = statusCache
        )

        (context.getSystemService(BluetoothManager::class.java) as BluetoothManager).adapter.enable()
        shadowOf(context.getSystemService(LocationManager::class.java) as LocationManager).apply {
            setLocationEnabled(true)
            setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        }

        val result = async { repository.getStatus().take(2).toList() }
        yield()

        shadowOf(context.getSystemService(LocationManager::class.java) as LocationManager).apply {
            setLocationEnabled(false)
            setProviderEnabled(LocationManager.NETWORK_PROVIDER, false)
        }
        context.sendBroadcast(Intent(LocationManager.MODE_CHANGED_ACTION))

        assertEquals(
            listOf(StatusResult.Enabled, StatusResult.InvalidPreconditions),
            result.await()
        )
    }

    @Test
    fun `getStatus re-triggers when bluetooth state changes change`() = runBlockingTest {
        val api = object : FakeExposureNotificationApi() {
            override suspend fun getStatus() = StatusResult.Enabled
        }
        val context = ApplicationProvider.getApplicationContext<Application>()
        val sharedPrefs = context.getSharedPreferences("repository_test", 0)
        val statusCache = StatusCache(sharedPrefs).apply {
            updateCachedStatus(StatusCache.CachedStatus.ENABLED)
        }

        val repository = createRepository(
            context = context,
            api = api,
            preferences = sharedPrefs,
            statusCache = statusCache
        )

        (context.getSystemService(BluetoothManager::class.java) as BluetoothManager).adapter.enable()
        shadowOf(context.getSystemService(LocationManager::class.java) as LocationManager).apply {
            setLocationEnabled(true)
            setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        }

        val result = async { repository.getStatus().take(2).toList() }
        yield()

        (context.getSystemService(BluetoothManager::class.java) as BluetoothManager).adapter.disable()
        context.sendBroadcast(Intent(LocationManager.MODE_CHANGED_ACTION))

        assertEquals(
            listOf(StatusResult.Enabled, StatusResult.InvalidPreconditions),
            result.await()
        )
    }

    @Test
    fun `getStatus does not remember API errors`() = runBlockingTest {
        val api = object : FakeExposureNotificationApi() {
            override suspend fun getStatus() = StatusResult.Unavailable(5)
        }
        val context = ApplicationProvider.getApplicationContext<Application>()
        val sharedPrefs = context.getSharedPreferences("repository_test", 0)
        val statusCache = StatusCache(sharedPrefs).apply {
            updateCachedStatus(StatusCache.CachedStatus.ENABLED)
        }

        val repository =
            createRepository(context, api, preferences = sharedPrefs, statusCache = statusCache)

        (context.getSystemService(BluetoothManager::class.java) as BluetoothManager).adapter.enable()
        shadowOf(context.getSystemService(LocationManager::class.java) as LocationManager).apply {
            setLocationEnabled(true)
            setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        }

        val result = async { repository.getStatus().take(2).toList() }
        yield()

        (context.getSystemService(BluetoothManager::class.java) as BluetoothManager).adapter.disable()
        context.sendBroadcast(Intent(LocationManager.MODE_CHANGED_ACTION))

        assertEquals(listOf(StatusResult.Enabled, StatusResult.Unavailable(5)), result.await())
        assertEquals(StatusCache.CachedStatus.ENABLED, statusCache.getCachedStatus().first())
    }

    @Test
    fun `getStatus without cached state defaults to disabled and retrieves status`() =
        runBlockingTest {
            val api = object : FakeExposureNotificationApi() {
                override suspend fun getStatus() = StatusResult.Enabled
            }
            val context = ApplicationProvider.getApplicationContext<Application>()
            val sharedPrefs = context.getSharedPreferences("repository_test", 0)
            val statusCache = StatusCache(sharedPrefs) // no initial value set

            (context.getSystemService(BluetoothManager::class.java) as BluetoothManager).adapter.enable()
            shadowOf(context.getSystemService(LocationManager::class.java) as LocationManager).apply {
                setLocationEnabled(true)
                setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
            }

            val repository =
                createRepository(context, api, preferences = sharedPrefs, statusCache = statusCache)

            val result = async { repository.getStatus().take(2).toList() }
            yield()

            (context.getSystemService(BluetoothManager::class.java) as BluetoothManager).adapter.disable()
            context.sendBroadcast(Intent(LocationManager.MODE_CHANGED_ACTION))

            assertEquals(listOf(StatusResult.Disabled, StatusResult.Enabled), result.await())
        }

    @Test
    fun `requestEnableNotificationsForcingConsent disables then enables the EN api`() =
        runBlocking {
            val disableCalled = AtomicBoolean(false)
            val enableCalled = AtomicBoolean(false)

            val api = object : FakeExposureNotificationApi() {
                override suspend fun getStatus() = StatusResult.Enabled
                override suspend fun disableNotifications(): DisableNotificationsResult {
                    disableCalled.set(true)
                    return DisableNotificationsResult.Disabled
                }

                override suspend fun requestEnableNotifications(): EnableNotificationsResult {
                    enableCalled.set(true)
                    return EnableNotificationsResult.Enabled
                }
            }

            val repository = createRepository(api = api, cdnService = object : CdnService {
                override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                    throw NotImplementedError()
                }

                override suspend fun getManifest(cacheHeader: String?): Manifest =
                    Manifest(listOf(), "res", "risk", "config")

                override suspend fun getRiskCalculationParameters(id: String): RiskCalculationParameters {
                    throw NotImplementedError()
                }

                override suspend fun getAppConfig(id: String, cacheHeader: String?): AppConfig =
                    AppConfig()
            }, scheduler = object : ProcessManifestWorkerScheduler {
                override fun schedule(intervalMinutes: Int) {
                }

                override fun cancel() {
                    throw AssertionError()
                }
            })

            repository.requestEnableNotificationsForcingConsent()
            assertTrue(disableCalled.get())
            assertTrue(enableCalled.get())
        }

    @Test
    fun `getDaysSinceLastExposure returns days when an exposure is reported`() = runBlocking {
        val preferences = context.getSharedPreferences("repository_test", 0)
        val clock =
            Clock.fixed(Instant.parse("2020-06-20T10:15:30.00Z"), ZoneId.of("Europe/Amsterdam"))

        val exposureDate = LocalDate.now(clock).minusDays(2)
        preferences.edit {
            putLong("last_token_exposure_date", exposureDate.toEpochDay())
            putString("last_token_id", "dummy")
        }

        val repository = createRepository(preferences = preferences, clock = clock)

        val daysSinceLastExposure = repository.getDaysSinceLastExposure()

        assertEquals(2, daysSinceLastExposure)
    }

    @Test
    fun `getDaysSinceLastExposure returns null when no exposure is reported`() = runBlocking {
        val repository = createRepository()
        assertNull(repository.getDaysSinceLastExposure())
    }

    private fun createRepository(
        context: Context = ApplicationProvider.getApplicationContext(),
        api: ExposureNotificationApi = FakeExposureNotificationApi(),
        cdnService: CdnService = mock(),
        preferences: SharedPreferences = context.getSharedPreferences("repository_test", 0),
        statusCache: StatusCache = StatusCache(
            preferences
        ),
        appLifecycleManager: AppLifecycleManager = AppLifecycleManager(
            preferences,
            mock()
        ) {},
        clock: Clock = Clock.systemDefaultZone(),
        lifecycleOwner: LifecycleOwner = TestLifecycleOwner(Lifecycle.State.STARTED),
        signatureValidation: Boolean = false,
        scheduler: ProcessManifestWorkerScheduler = fakeScheduler,
        appConfigManager: AppConfigManager = AppConfigManager(cdnService)
    ): ExposureNotificationsRepository {
        return ExposureNotificationsRepository(
            context,
            api,
            cdnService,
            preferences,
            scheduler,
            appLifecycleManager,
            statusCache,
            appConfigManager,
            clock,
            lifecycleOwner = lifecycleOwner,
            signatureValidation = signatureValidation
        )
    }
}

private class TestLifecycleOwner(private val state: Lifecycle.State) : LifecycleOwner {
    override fun getLifecycle(): Lifecycle {
        return object : Lifecycle() {
            override fun addObserver(observer: LifecycleObserver) {
                if (observer is DefaultLifecycleObserver) {
                    when (currentState) {
                        State.DESTROYED -> observer.onDestroy(this@TestLifecycleOwner)
                        State.INITIALIZED -> { /* nothing */
                        }
                        State.CREATED -> observer.onCreate(this@TestLifecycleOwner)
                        State.STARTED -> observer.onStart(this@TestLifecycleOwner)
                        State.RESUMED -> observer.onResume(this@TestLifecycleOwner)
                    }
                }
            }

            override fun removeObserver(observer: LifecycleObserver) {
            }

            override fun getCurrentState(): State = state
        }
    }
}
