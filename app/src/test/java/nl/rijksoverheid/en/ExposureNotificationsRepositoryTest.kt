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
import androidx.core.content.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.exposurenotification.DailySummariesConfig
import com.google.android.gms.nearby.exposurenotification.DiagnosisKeysDataMapping
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import com.google.android.gms.nearby.exposurenotification.ReportType
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.yield
import nl.rijksoverheid.en.api.CacheStrategy
import nl.rijksoverheid.en.api.CdnService
import nl.rijksoverheid.en.api.model.AppConfig
import nl.rijksoverheid.en.api.model.DaySinceOnsetToInfectiousness
import nl.rijksoverheid.en.api.model.Manifest
import nl.rijksoverheid.en.api.model.ResourceBundle
import nl.rijksoverheid.en.api.model.RiskCalculationParameters
import nl.rijksoverheid.en.applifecycle.AppLifecycleManager
import nl.rijksoverheid.en.config.AppConfigManager
import nl.rijksoverheid.en.enapi.DailyRiskScoresResult
import nl.rijksoverheid.en.enapi.DiagnosisKeysResult
import nl.rijksoverheid.en.enapi.DisableNotificationsResult
import nl.rijksoverheid.en.enapi.EnableNotificationsResult
import nl.rijksoverheid.en.enapi.ExposureNotificationApi
import nl.rijksoverheid.en.enapi.StatusResult
import nl.rijksoverheid.en.enapi.nearby.DailyRiskScores
import nl.rijksoverheid.en.job.BackgroundWorkScheduler
import nl.rijksoverheid.en.notifier.NotificationsRepository
import nl.rijksoverheid.en.preferences.AsyncSharedPreferences
import nl.rijksoverheid.en.signing.ResponseSignatureValidator
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
import retrofit2.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

private val MOCK_RISK_PARAMS_RESPONSE = MockResponse().setBody(
    """
   {
        "daysSinceOnsetToInfectiousness": [
            {"daysSinceOnsetOfSymptoms": -14, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": -13, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": -12, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": -11, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": -10, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": -9, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": -8, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": -7, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": -6, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": -5, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": -4, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": -3, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": -2, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": -1, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": 0, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": 1, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": 2, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": 3, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": 4, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": 5, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": 6, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": 7, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": 8, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": 9, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": 10, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": 11, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": 12, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": 13, "infectiousness":1},
            {"daysSinceOnsetOfSymptoms": 14, "infectiousness":1}
        ],
        "infectiousnessWhenDaysSinceOnsetMissing": 1,
        "minimumWindowScore": 0.0,
        "daysSinceExposureThreshold": 10,
        "attenuationBucketThresholds": [56, 62, 70],
        "attenuationBucketWeights": [1.0, 1.0, 0.3, 0.0],
        "infectiousnessWeights": [0.0, 1.0, 2.0],
        "reportTypeWeights": [0.0, 1.0, 1.0, 0.0, 0.0, 0.0],
        "minimumRiskScore": 1.0,
        "reportTypeWhenMissing": 1
   }
    """.trimIndent()
)

private val MOCK_RISK_CALCULATION_PARAMS = RiskCalculationParameters(
    reportTypeWeights = listOf(0.0, 1.0, 0.0, 0.0, 0.0, 0.0),
    infectiousnessWeights = listOf(0.0, 1.0, 1.0),
    attenuationBucketThresholds = listOf(56, 62, 70),
    attenuationBucketWeights = listOf(1.0, 1.0, 1.0, 0.0),
    daysSinceExposureThreshold = 0,
    minimumWindowScore = 300.0,
    minimumRiskScore = 900.0,
    daysSinceOnsetToInfectiousness = listOf(
        DaySinceOnsetToInfectiousness(-14, 0),
        DaySinceOnsetToInfectiousness(-13, 0),
        DaySinceOnsetToInfectiousness(-12, 0),
        DaySinceOnsetToInfectiousness(-11, 0),
        DaySinceOnsetToInfectiousness(-10, 0),
        DaySinceOnsetToInfectiousness(-10, 0),
        DaySinceOnsetToInfectiousness(-9, 0),
        DaySinceOnsetToInfectiousness(-8, 0),
        DaySinceOnsetToInfectiousness(-7, 0),
        DaySinceOnsetToInfectiousness(-6, 0),
        DaySinceOnsetToInfectiousness(-5, 0),
        DaySinceOnsetToInfectiousness(-4, 0),
        DaySinceOnsetToInfectiousness(-3, 0),
        DaySinceOnsetToInfectiousness(-2, 1),
        DaySinceOnsetToInfectiousness(-1, 1),
        DaySinceOnsetToInfectiousness(0, 1),
        DaySinceOnsetToInfectiousness(1, 1),
        DaySinceOnsetToInfectiousness(2, 1),
        DaySinceOnsetToInfectiousness(3, 1),
        DaySinceOnsetToInfectiousness(4, 1),
        DaySinceOnsetToInfectiousness(5, 1),
        DaySinceOnsetToInfectiousness(6, 1),
        DaySinceOnsetToInfectiousness(7, 1),
        DaySinceOnsetToInfectiousness(8, 1),
        DaySinceOnsetToInfectiousness(9, 1),
        DaySinceOnsetToInfectiousness(10, 1),
        DaySinceOnsetToInfectiousness(11, 1),
        DaySinceOnsetToInfectiousness(12, 0),
        DaySinceOnsetToInfectiousness(13, 0),
        DaySinceOnsetToInfectiousness(14, 0),

    ),
    infectiousnessWhenDaysSinceOnsetMissing = Infectiousness.STANDARD,
    reportTypeWhenMissing = ReportType.CONFIRMED_TEST
)

@RunWith(RobolectricTestRunner::class)
class ExposureNotificationsRepositoryTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var context: Context

    private val fakeScheduler = object : BackgroundWorkScheduler {
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
    fun `processExposureKeySets marks all keys processed on first run`() = runBlocking {
        mockWebServer.start()

        val context = ApplicationProvider.getApplicationContext<Application>()
        val service = CdnService.create(
            context,
            BuildConfig.VERSION_CODE,
            OkHttpClient(),
            mockWebServer.url("/").toString()
        )

        val api = object : FakeExposureNotificationApi() {
            override suspend fun getStatus(): StatusResult = StatusResult.Enabled
            override suspend fun provideDiagnosisKeys(
                files: List<File>,
                diagnosisKeysDataMapping: DiagnosisKeysDataMapping
            ): DiagnosisKeysResult = throw AssertionError()
        }

        val sharedPrefs = ApplicationProvider.getApplicationContext<Application>()
            .getSharedPreferences("repository_test", 0)

        val repository =
            createRepository(api = api, cdnService = service, preferences = sharedPrefs)

        val result =
            repository.processExposureKeySets(
                Manifest(
                    listOf("test"),
                    "config-params",
                    "appConfigId"
                )
            )

        assertEquals(0, mockWebServer.requestCount)
        assertTrue(result is ProcessExposureKeysResult.Success)
        assertEquals(setOf("test"), sharedPrefs.getStringSet("exposure_key_sets", emptySet()))
    }

    @Test
    fun `processExposureKeySets processes exposure key sets`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("dummy_key_file"))
        mockWebServer.enqueue(MOCK_RISK_PARAMS_RESPONSE)
        mockWebServer.start()
        val context = ApplicationProvider.getApplicationContext<Application>()
        val service = CdnService.create(
            context,
            BuildConfig.VERSION_CODE,
            OkHttpClient(),
            mockWebServer.url("/").toString()
        )
        val config = AtomicReference<DiagnosisKeysDataMapping>()

        val api = object : FakeExposureNotificationApi() {
            override suspend fun getStatus(): StatusResult = StatusResult.Enabled
            override suspend fun provideDiagnosisKeys(
                files: List<File>,
                diagnosisKeysDataMapping: DiagnosisKeysDataMapping
            ): DiagnosisKeysResult {
                if (files.size != 1) throw AssertionError("Expected one file")
                config.set(diagnosisKeysDataMapping)
                return DiagnosisKeysResult.Success
            }
        }

        val sharedPrefs = ApplicationProvider.getApplicationContext<Application>()
            .getSharedPreferences("repository_test", 0)

        sharedPrefs.edit {
            putStringSet("exposure_key_sets", emptySet())
        }

        val repository =
            createRepository(api = api, cdnService = service, preferences = sharedPrefs)

        val result =
            repository.processExposureKeySets(
                Manifest(
                    listOf("test"),
                    "config-params",
                    "appConfigId"
                )
            )

        assertEquals(
            DiagnosisKeysDataMapping.DiagnosisKeysDataMappingBuilder()
                .setDaysSinceOnsetToInfectiousness((-14..14).map { it to Infectiousness.STANDARD }.toMap())
                .setReportTypeWhenMissing(ReportType.CONFIRMED_TEST)
                .setInfectiousnessWhenDaysSinceOnsetMissing(Infectiousness.STANDARD).build(),
            config.get()
        )
        assertEquals(2, mockWebServer.requestCount)
        assertEquals(ProcessExposureKeysResult.Success, result)
        assertEquals("/v4/exposurekeyset/test", mockWebServer.takeRequest().path)
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
            BuildConfig.VERSION_CODE,
            OkHttpClient(),
            mockWebServer.url("/").toString()
        )

        val api = object : FakeExposureNotificationApi() {
            override suspend fun getStatus(): StatusResult = StatusResult.Enabled
            override suspend fun provideDiagnosisKeys(
                files: List<File>,
                diagnosisKeysDataMapping: DiagnosisKeysDataMapping
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
                "config-params",
                "appConfigId"
            )
        )

        assertEquals(2, mockWebServer.requestCount)
        assertEquals(ProcessExposureKeysResult.Success, result)

        assertEquals("/v4/exposurekeyset/test2", mockWebServer.takeRequest().path)
        assertEquals(
            "/v4/riskcalculationparameters/config-params",
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
            BuildConfig.VERSION_CODE,
            OkHttpClient(),
            mockWebServer.url("/").toString()
        )

        val api = object : FakeExposureNotificationApi() {
            override suspend fun provideDiagnosisKeys(
                files: List<File>,
                diagnosisKeysDataMapping: DiagnosisKeysDataMapping
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
                BuildConfig.VERSION_CODE,
                OkHttpClient(),
                mockWebServer.url("/").toString()
            )

            val api = object : FakeExposureNotificationApi() {
                override suspend fun getStatus(): StatusResult = StatusResult.Enabled

                override suspend fun provideDiagnosisKeys(
                    files: List<File>,
                    diagnosisKeysDataMapping: DiagnosisKeysDataMapping
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
                BuildConfig.VERSION_CODE,
                OkHttpClient(),
                mockWebServer.url("/").toString()
            )

            val api = object : FakeExposureNotificationApi() {
                override suspend fun getStatus(): StatusResult = StatusResult.Enabled
                override suspend fun provideDiagnosisKeys(
                    files: List<File>,
                    diagnosisKeysDataMapping: DiagnosisKeysDataMapping
                ): DiagnosisKeysResult {
                    throw java.lang.AssertionError("Should not be processed")
                }
            }

            val sharedPrefs = ApplicationProvider.getApplicationContext<Application>()
                .getSharedPreferences("repository_test", 0)

            sharedPrefs.edit {
                putStringSet("exposure_key_sets", emptySet())
            }

            val repository =
                createRepository(api = api, cdnService = service, preferences = sharedPrefs)

            val result =
                repository.processExposureKeySets(
                    Manifest(
                        listOf("test"),
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
                BuildConfig.VERSION_CODE,
                OkHttpClient(),
                mockWebServer.url("/").toString()
            )

            val api = object : FakeExposureNotificationApi() {
                override suspend fun getStatus(): StatusResult = StatusResult.Enabled

                override suspend fun provideDiagnosisKeys(
                    files: List<File>,
                    diagnosisKeysDataMapping: DiagnosisKeysDataMapping
                ): DiagnosisKeysResult {
                    throw java.lang.AssertionError("Should not be processed")
                }
            }

            val sharedPrefs = ApplicationProvider.getApplicationContext<Application>()
                .getSharedPreferences("repository_test", 0)

            sharedPrefs.edit {
                putStringSet("exposure_key_sets", emptySet())
            }

            val repository = createRepository(
                api = api,
                cdnService = service,
                preferences = sharedPrefs,
                signatureValidation = true,
                signatureValidator = ResponseSignatureValidator(createTrustManager())
            )

            val result =
                repository.processExposureKeySets(
                    Manifest(
                        listOf("test"),
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
                BuildConfig.VERSION_CODE,
                OkHttpClient(),
                mockWebServer.url("/").toString()
            )

            val api = object : FakeExposureNotificationApi() {
                override suspend fun getStatus(): StatusResult = StatusResult.Enabled
                override suspend fun provideDiagnosisKeys(
                    files: List<File>,
                    diagnosisKeysDataMapping: DiagnosisKeysDataMapping
                ): DiagnosisKeysResult {
                    throw java.lang.AssertionError("Should not be processed")
                }
            }

            val sharedPrefs = ApplicationProvider.getApplicationContext<Application>()
                .getSharedPreferences("repository_test", 0)

            sharedPrefs.edit {
                putStringSet("exposure_key_sets", emptySet())
            }

            val repository = createRepository(
                api = api,
                cdnService = service,
                signatureValidation = true,
                signatureValidator = ResponseSignatureValidator(createTrustManager())
            )

            val result =
                repository.processExposureKeySets(
                    Manifest(
                        listOf("test"),
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
                        "/v4/exposurekeyset/test" -> {
                            MockResponse().setResponseCode(500)
                        }
                        "/v4/exposurekeyset/test2" -> {
                            MockResponse().setBody("dummy_key_file")
                        }
                        "/v4/riskcalculationparameters/config-params" -> MOCK_RISK_PARAMS_RESPONSE

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
                BuildConfig.VERSION_CODE,
                OkHttpClient(),
                mockWebServer.url("/").toString()
            )

            val processed = AtomicBoolean(false)
            val api = object : FakeExposureNotificationApi() {
                override suspend fun getStatus(): StatusResult = StatusResult.Enabled
                override suspend fun provideDiagnosisKeys(
                    files: List<File>,
                    diagnosisKeysDataMapping: DiagnosisKeysDataMapping
                ): DiagnosisKeysResult {
                    if (files.size != 1) throw java.lang.AssertionError()
                    processed.set(true)
                    return DiagnosisKeysResult.Success
                }
            }

            val sharedPrefs = ApplicationProvider.getApplicationContext<Application>()
                .getSharedPreferences("repository_test", 0)

            sharedPrefs.edit {
                putStringSet("exposure_key_sets", emptySet())
            }

            val repository =
                createRepository(api = api, cdnService = service, preferences = sharedPrefs)

            val result = repository.processExposureKeySets(
                Manifest(
                    listOf("test", "test2"),
                    "config-params",
                    "appConfigId"
                )
            )

            val request1 = mockWebServer.takeRequest()
            val request2 = mockWebServer.takeRequest()
            val requests = listOf(request1, request2).sortedBy { it.path }

            assertEquals("/v4/exposurekeyset/test", requests[0].path)
            assertEquals("/v4/exposurekeyset/test2", requests[1].path)
            assertEquals(
                "/v4/riskcalculationparameters/config-params",
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
        val clock = Clock.fixed(Instant.parse("2020-06-20T10:15:30.00Z"), ZoneId.of("UTC"))

        val api = object : FakeExposureNotificationApi() {
            override suspend fun getDailyRiskScores(
                config: DailySummariesConfig
            ): DailyRiskScoresResult {
                return DailyRiskScoresResult.Success(
                    listOf(
                        DailyRiskScores(
                            LocalDate.now(clock).minusDays(4).toEpochDay(),
                            500.0,
                            950.0
                        )
                    )
                )
            }
        }

        val fakeService = object : CdnService {
            override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                throw NotImplementedError()
            }

            override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest =
                Manifest(emptyList(), "test-params", "")

            override suspend fun getRiskCalculationParameters(
                id: String,
                cacheStrategy: CacheStrategy?
            ): RiskCalculationParameters {
                return MOCK_RISK_CALCULATION_PARAMS
            }

            override suspend fun getAppConfig(id: String, cacheStrategy: CacheStrategy?) =
                throw NotImplementedError()

            override suspend fun getResourceBundle(
                id: String,
                cacheStrategy: CacheStrategy?
            ): ResourceBundle {
                throw java.lang.IllegalStateException()
            }
        }

        val repository = createRepository(
            api = api,
            clock = clock,
            cdnService = fakeService
        )

        val result = repository.addExposure()

        assertTrue(result is AddExposureResult.Notify)
        assertEquals(
            LocalDate.of(2020, 6, 20).minusDays(4),
            repository.getLastExposureDate().filterNotNull().first()
        )
    }

    @Test
    fun `addExposure while newer exposure exists keeps newer exposure`() = runBlocking {
        val clock = Clock.fixed(Instant.parse("2020-06-20T10:15:30.00Z"), ZoneId.of("UTC"))

        val api = object : FakeExposureNotificationApi() {
            val getDailyRiskScoresResults = listOf(
                listOf(
                    DailyRiskScores(
                        LocalDate.now(clock).minusDays(8).toEpochDay(),
                        500.0,
                        950.0
                    )
                ),
                listOf(
                    DailyRiskScores(
                        LocalDate.now(clock).minusDays(4).toEpochDay(),
                        500.0,
                        950.0
                    )
                )
            ).iterator()

            override suspend fun getDailyRiskScores(
                config: DailySummariesConfig
            ): DailyRiskScoresResult {
                return DailyRiskScoresResult.Success(getDailyRiskScoresResults.next())
            }
        }

        val fakeService = object : CdnService {
            override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                throw NotImplementedError()
            }

            override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest =
                Manifest(emptyList(), "test-params", "")

            override suspend fun getRiskCalculationParameters(
                id: String,
                cacheStrategy: CacheStrategy?
            ): RiskCalculationParameters {
                return MOCK_RISK_CALCULATION_PARAMS
            }

            override suspend fun getAppConfig(id: String, cacheStrategy: CacheStrategy?) =
                throw NotImplementedError()

            override suspend fun getResourceBundle(
                id: String,
                cacheStrategy: CacheStrategy?
            ): ResourceBundle {
                throw java.lang.IllegalStateException()
            }
        }

        val repository = createRepository(
            api = api,
            clock = clock,
            cdnService = fakeService
        )

        repository.addExposure()
        repository.addExposure()

        assertEquals(
            LocalDate.of(2020, 6, 20).minusDays(4),
            repository.getLastExposureDate().filterNotNull().first()
        )
    }

    @Test
    fun `addExposure without exposure windows is ignored`() = runBlocking {
        val dateTime = "2020-06-20T10:15:30.00Z"

        val api = object : FakeExposureNotificationApi() {
            override suspend fun getDailyRiskScores(config: DailySummariesConfig): DailyRiskScoresResult {
                return DailyRiskScoresResult.Success(emptyList())
            }
        }

        val fakeService = object : CdnService {
            override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                throw NotImplementedError()
            }

            override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest =
                Manifest(emptyList(), "test-params", "")

            override suspend fun getRiskCalculationParameters(
                id: String,
                cacheStrategy: CacheStrategy?
            ): RiskCalculationParameters {
                return MOCK_RISK_CALCULATION_PARAMS
            }

            override suspend fun getAppConfig(id: String, cacheStrategy: CacheStrategy?) =
                throw NotImplementedError()

            override suspend fun getResourceBundle(
                id: String,
                cacheStrategy: CacheStrategy?
            ): ResourceBundle {
                throw java.lang.IllegalStateException()
            }
        }

        val repository = createRepository(
            api = api,
            clock = Clock.fixed(Instant.parse(dateTime), ZoneId.of("UTC")),
            cdnService = fakeService
        )

        repository.addExposure()

        assertEquals(null, repository.getLastExposureDate().first())
    }

    @Test
    fun `addExposure returns Error result when fetching risk scores fails`() = runBlocking {
        val dateTime = "2020-06-20T10:15:30.00Z"

        val api = object : FakeExposureNotificationApi() {
            override suspend fun getDailyRiskScores(config: DailySummariesConfig): DailyRiskScoresResult {
                return DailyRiskScoresResult.UnknownError(ApiException(Status.RESULT_INTERNAL_ERROR))
            }
        }

        val fakeService = object : CdnService {
            override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                throw NotImplementedError()
            }

            override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest =
                Manifest(emptyList(), "test-params", "")

            override suspend fun getRiskCalculationParameters(
                id: String,
                cacheStrategy: CacheStrategy?
            ): RiskCalculationParameters {
                return MOCK_RISK_CALCULATION_PARAMS
            }

            override suspend fun getAppConfig(id: String, cacheStrategy: CacheStrategy?) =
                throw NotImplementedError()

            override suspend fun getResourceBundle(id: String, cacheStrategy: CacheStrategy?): ResourceBundle {
                throw NotImplementedError()
            }
        }

        val repository = createRepository(
            api = api,
            clock = Clock.fixed(Instant.parse(dateTime), ZoneId.of("UTC")),
            cdnService = fakeService
        )

        val result = repository.addExposure()

        assertEquals(AddExposureResult.Error, result)
    }

    @Test
    fun `addExposure risk score below threshold is ignored`() = runBlocking {
        val clock = Clock.fixed(Instant.parse("2020-06-20T10:15:30.00Z"), ZoneId.of("UTC"))

        val api = object : FakeExposureNotificationApi() {
            override suspend fun getDailyRiskScores(
                config: DailySummariesConfig
            ): DailyRiskScoresResult {
                return DailyRiskScoresResult.Success(
                    listOf(
                        DailyRiskScores(
                            LocalDate.now(clock).minusDays(4).toEpochDay(),
                            2.0,
                            2.0
                        )
                    )
                )
            }
        }

        val fakeService = object : CdnService {
            override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                throw NotImplementedError()
            }

            override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest =
                Manifest(emptyList(), "test-params", "")

            override suspend fun getRiskCalculationParameters(
                id: String,
                cacheStrategy: CacheStrategy?
            ): RiskCalculationParameters {
                return MOCK_RISK_CALCULATION_PARAMS
            }

            override suspend fun getAppConfig(id: String, cacheStrategy: CacheStrategy?) =
                throw NotImplementedError()

            override suspend fun getResourceBundle(
                id: String,
                cacheStrategy: CacheStrategy?
            ): ResourceBundle {
                throw java.lang.IllegalStateException()
            }
        }

        val repository = createRepository(
            api = api,
            clock = clock,
            cdnService = fakeService
        )

        repository.addExposure()

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

                override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest =
                    Manifest(emptyList(), "", "appConfig")

                override suspend fun getRiskCalculationParameters(
                    id: String,
                    cacheStrategy: CacheStrategy?
                ): RiskCalculationParameters {
                    throw NotImplementedError()
                }

                override suspend fun getAppConfig(id: String, cacheStrategy: CacheStrategy?) =
                    AppConfig(1, 5, 0.0)

                override suspend fun getResourceBundle(
                    id: String,
                    cacheStrategy: CacheStrategy?
                ): ResourceBundle {
                    throw java.lang.IllegalStateException()
                }
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
    fun `processManifest fetches the latest resource bundle`() =
        runBlocking {
            val dateTime = "2020-06-20T10:15:30.00Z"
            val called = AtomicBoolean(false)
            val fakeService = object : CdnService {
                override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                    throw NotImplementedError()
                }

                override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest =
                    Manifest(emptyList(), "", "appConfig", resourceBundleId = "bundle")

                override suspend fun getRiskCalculationParameters(
                    id: String,
                    cacheStrategy: CacheStrategy?
                ): RiskCalculationParameters {
                    throw NotImplementedError()
                }

                override suspend fun getAppConfig(id: String, cacheStrategy: CacheStrategy?) =
                    AppConfig(1, 5, 0.0)

                override suspend fun getResourceBundle(
                    id: String,
                    cacheStrategy: CacheStrategy?
                ): ResourceBundle {
                    called.set(true)
                    return ResourceBundle(emptyMap(), ResourceBundle.Guidance(emptyList(), emptyList()))
                }
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

            repository.processManifest()
            assertTrue(called.get())
        }

    @Test
    fun `processManifest stops processing when the app is disabled`() =
        runBlocking {
            val dateTime = "2020-06-20T10:15:30.00Z"
            val fakeService = object : CdnService {
                override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                    throw NotImplementedError()
                }

                override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest =
                    Manifest(emptyList(), "riskParamId", "configId")

                override suspend fun getRiskCalculationParameters(
                    id: String,
                    cacheStrategy: CacheStrategy?
                ): RiskCalculationParameters {
                    throw NotImplementedError()
                }

                override suspend fun getAppConfig(
                    id: String,
                    cacheStrategy: CacheStrategy?
                ): AppConfig =
                    AppConfig(1, 5, 0.0, coronaMelderDeactivated = "deactivated")

                override suspend fun getResourceBundle(
                    id: String,
                    cacheStrategy: CacheStrategy?
                ): ResourceBundle {
                    throw IllegalStateException()
                }
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
                scheduler = object : BackgroundWorkScheduler {
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
                BuildConfig.VERSION_CODE,
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

                override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest =
                    Manifest(emptyList(), "riskParamId", "configId")

                override suspend fun getRiskCalculationParameters(
                    id: String,
                    cacheStrategy: CacheStrategy?
                ): RiskCalculationParameters {
                    throw NotImplementedError()
                }

                override suspend fun getAppConfig(
                    id: String,
                    cacheStrategy: CacheStrategy?
                ): AppConfig =
                    AppConfig(BuildConfig.VERSION_CODE + 1, 5, 0.0)

                override suspend fun getResourceBundle(
                    id: String,
                    cacheStrategy: CacheStrategy?
                ): ResourceBundle {
                    throw java.lang.IllegalStateException()
                }
            }

            val context = ApplicationProvider.getApplicationContext<Application>()

            val repository = createRepository(
                context,
                api = object : FakeExposureNotificationApi() {
                    override suspend fun getStatus(): StatusResult = StatusResult.Enabled
                },
                appLifecycleManager = AppLifecycleManager(
                    context,
                    context.getSharedPreferences(
                        "test_config",
                        0
                    ),
                    mock()
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

                override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest =
                    Manifest(emptyList(), "riskParamId", "configId")

                override suspend fun getRiskCalculationParameters(
                    id: String,
                    cacheStrategy: CacheStrategy?
                ): RiskCalculationParameters {
                    throw NotImplementedError()
                }

                override suspend fun getAppConfig(
                    id: String,
                    cacheStrategy: CacheStrategy?
                ): AppConfig =
                    AppConfig(0, 5, 0.0)

                override suspend fun getResourceBundle(
                    id: String,
                    cacheStrategy: CacheStrategy?
                ): ResourceBundle {
                    throw java.lang.IllegalStateException()
                }
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

                    override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest =
                        Manifest(
                            listOf(), "risk", "config"
                        )

                    override suspend fun getRiskCalculationParameters(
                        id: String,
                        cacheStrategy: CacheStrategy?
                    ): RiskCalculationParameters {
                        throw NotImplementedError()
                    }

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
                scheduler = object : BackgroundWorkScheduler {
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
    fun `getLastExposureDate returns date added through addExposure`() =
        runBlocking {
            val clock = Clock.fixed(Instant.parse("2020-06-20T10:15:30.00Z"), ZoneId.of("UTC"))

            val sharedPrefs = ApplicationProvider.getApplicationContext<Application>()
                .getSharedPreferences("repository_test", 0)

            val api = object : FakeExposureNotificationApi() {
                override suspend fun getDailyRiskScores(
                    config: DailySummariesConfig
                ): DailyRiskScoresResult {
                    return DailyRiskScoresResult.Success(
                        listOf(
                            DailyRiskScores(
                                LocalDate.now(clock).minusDays(4).toEpochDay(),
                                500.0,
                                950.0
                            )
                        )
                    )
                }
            }

            val fakeService = object : CdnService {
                override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                    throw NotImplementedError()
                }

                override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest =
                    Manifest(emptyList(), "test-params", "")

                override suspend fun getRiskCalculationParameters(
                    id: String,
                    cacheStrategy: CacheStrategy?
                ): RiskCalculationParameters {
                    return MOCK_RISK_CALCULATION_PARAMS
                }

                override suspend fun getAppConfig(id: String, cacheStrategy: CacheStrategy?) =
                    throw NotImplementedError()

                override suspend fun getResourceBundle(
                    id: String,
                    cacheStrategy: CacheStrategy?
                ): ResourceBundle {
                    throw java.lang.IllegalStateException()
                }
            }

            val repository = createRepository(
                api = api,
                preferences = sharedPrefs,
                clock = clock,
                cdnService = fakeService
            )

            repository.addExposure()

            val result = repository.getLastExposureDate().first()
            assertEquals(LocalDate.now(clock).minusDays(4), result)
        }

    @Test
    fun `cleanupPreviouslyKnownExposureDate removes old previouslyKnownExposureDate added through addExposure`() =
        runBlocking {
            val clock = Clock.fixed(Instant.parse("2020-06-20T10:15:30.00Z"), ZoneId.of("UTC"))

            val sharedPrefs = ApplicationProvider.getApplicationContext<Application>()
                .getSharedPreferences("repository_test", 0)

            val api = object : FakeExposureNotificationApi() {
                override suspend fun getDailyRiskScores(
                    config: DailySummariesConfig
                ): DailyRiskScoresResult {
                    return DailyRiskScoresResult.Success(
                        listOf(
                            DailyRiskScores(
                                LocalDate.now(clock).minusDays(15).toEpochDay(),
                                500.0,
                                950.0
                            )
                        )
                    )
                }
            }

            val fakeService = object : CdnService {
                override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                    throw NotImplementedError()
                }

                override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest =
                    Manifest(emptyList(), "test-params", "")

                override suspend fun getRiskCalculationParameters(
                    id: String,
                    cacheStrategy: CacheStrategy?
                ): RiskCalculationParameters {
                    return MOCK_RISK_CALCULATION_PARAMS
                }

                override suspend fun getAppConfig(id: String, cacheStrategy: CacheStrategy?) =
                    throw NotImplementedError()

                override suspend fun getResourceBundle(
                    id: String,
                    cacheStrategy: CacheStrategy?
                ): ResourceBundle {
                    throw java.lang.IllegalStateException()
                }
            }

            val repository = createRepository(
                api = api,
                preferences = sharedPrefs,
                clock = clock,
                cdnService = fakeService
            )

            // Verify previouslyKnownExposureDate was added
            repository.addExposure()
            assertEquals(LocalDate.now(clock).minusDays(15), repository.getPreviouslyKnownExposureDate())

            // Verify previouslyKnownExposureDate was removed
            repository.cleanupPreviouslyKnownExposures()
            assertEquals(null, repository.getPreviouslyKnownExposureDate())
        }

    @Test
    fun `addExposure won't trigger on deleted known exposures`() = runBlocking {
        val clock = Clock.fixed(Instant.parse("2020-06-20T10:15:30.00Z"), ZoneId.of("UTC"))

        val api = object : FakeExposureNotificationApi() {
            val getDailyRiskScoresResults = listOf(
                listOf(
                    DailyRiskScores(
                        LocalDate.now(clock).minusDays(8).toEpochDay(),
                        500.0,
                        950.0
                    )
                ),
                listOf(
                    DailyRiskScores(
                        LocalDate.now(clock).minusDays(8).toEpochDay(),
                        500.0,
                        950.0
                    ),
                    DailyRiskScores(
                        LocalDate.now(clock).minusDays(4).toEpochDay(),
                        100.0,
                        100.0
                    )
                )
            ).iterator()

            override suspend fun getDailyRiskScores(
                config: DailySummariesConfig
            ): DailyRiskScoresResult {
                return DailyRiskScoresResult.Success(getDailyRiskScoresResults.next())
            }
        }

        val fakeService = object : CdnService {
            override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                throw NotImplementedError()
            }

            override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest =
                Manifest(emptyList(), "test-params", "")

            override suspend fun getRiskCalculationParameters(
                id: String,
                cacheStrategy: CacheStrategy?
            ): RiskCalculationParameters {
                return MOCK_RISK_CALCULATION_PARAMS
            }

            override suspend fun getAppConfig(id: String, cacheStrategy: CacheStrategy?) =
                throw NotImplementedError()

            override suspend fun getResourceBundle(
                id: String,
                cacheStrategy: CacheStrategy?
            ): ResourceBundle {
                throw java.lang.IllegalStateException()
            }
        }

        val repository = createRepository(
            api = api,
            clock = clock,
            cdnService = fakeService
        )

        repository.addExposure()
        repository.resetExposures()

        val result = repository.addExposure()
        assertEquals(
            AddExposureResult.Processed,
            result
        )
    }

    @Test
    fun `keyProcessingOverdue returns true if last successful time of key processing is more than 24 hours in the past`() {
        val lastSyncDateTime = "2020-06-20T10:15:30.00Z"
        val dateTime = "2020-06-21T10:16:30.00Z"
        val fakeService = object : CdnService {
            override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                throw NotImplementedError()
            }

            override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest {
                throw NotImplementedError()
            }

            override suspend fun getRiskCalculationParameters(
                id: String,
                cacheStrategy: CacheStrategy?
            ): RiskCalculationParameters {
                throw NotImplementedError()
            }

            override suspend fun getAppConfig(
                id: String,
                cacheStrategy: CacheStrategy?
            ): AppConfig =
                AppConfig(1, 5, 0.0)

            override suspend fun getResourceBundle(
                id: String,
                cacheStrategy: CacheStrategy?
            ): ResourceBundle {
                throw java.lang.IllegalStateException()
            }
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

        runBlocking {
            assertTrue(repository.keyProcessingOverdue())
        }
    }

    @Test
    fun `keyProcessingOverdue returns false if last successful time of key processing is more than 24 hours in the past but enabledNotificationsDateTime isn't`() {
        val notificationsEnabledDateTime = "2020-06-21T10:10:30.00Z"
        val lastSyncDateTime = "2020-06-20T10:15:30.00Z"
        val dateTime = "2020-06-21T10:16:30.00Z"
        val fakeService = object : CdnService {
            override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                throw NotImplementedError()
            }

            override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest {
                throw NotImplementedError()
            }

            override suspend fun getRiskCalculationParameters(
                id: String,
                cacheStrategy: CacheStrategy?
            ): RiskCalculationParameters {
                throw NotImplementedError()
            }

            override suspend fun getAppConfig(
                id: String,
                cacheStrategy: CacheStrategy?
            ): AppConfig =
                AppConfig(1, 5, 0.0)

            override suspend fun getResourceBundle(
                id: String,
                cacheStrategy: CacheStrategy?
            ): ResourceBundle {
                throw java.lang.IllegalStateException()
            }
        }

        val context = ApplicationProvider.getApplicationContext<Application>()
        val sharedPrefs = context.getSharedPreferences("repository_test", 0)

        sharedPrefs.edit {
            putLong("last_keys_processed", Instant.parse(lastSyncDateTime).toEpochMilli())
            putLong("notifications_enabled_timestamp", Instant.parse(notificationsEnabledDateTime).toEpochMilli())
        }

        val repository = createRepository(
            context = context,
            preferences = sharedPrefs,
            cdnService = fakeService,
            clock = Clock.fixed(Instant.parse(dateTime), ZoneId.of("UTC"))
        )

        runBlocking {
            assertFalse(repository.keyProcessingOverdue())
        }
    }

    @Test
    fun `keyProcessingOverdue returns false if no timestamp is stored`() {
        val dateTime = "2020-06-21T10:15:30.00Z"
        val fakeService = object : CdnService {
            override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                throw NotImplementedError()
            }

            override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest {
                throw NotImplementedError()
            }

            override suspend fun getRiskCalculationParameters(
                id: String,
                cacheStrategy: CacheStrategy?
            ): RiskCalculationParameters {
                throw NotImplementedError()
            }

            override suspend fun getAppConfig(
                id: String,
                cacheStrategy: CacheStrategy?
            ): AppConfig =
                AppConfig(1, 5, 0.0)

            override suspend fun getResourceBundle(
                id: String,
                cacheStrategy: CacheStrategy?
            ): ResourceBundle {
                throw java.lang.IllegalStateException()
            }
        }

        val repository = createRepository(
            cdnService = fakeService,
            clock = Clock.fixed(Instant.parse(dateTime), ZoneId.of("UTC"))
        )

        runBlocking {
            assertFalse(repository.keyProcessingOverdue())
        }
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
            ),
            result.await()
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
            listOf(StatusResult.Enabled, StatusResult.LocationPreconditionNotSatisfied),
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
            listOf(StatusResult.Enabled, StatusResult.BluetoothDisabled),
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
    fun `requestEnableNotifications updates statusCache to BluetoothDisabled when bluetooth is disabled`() = runBlockingTest {
        val api = object : FakeExposureNotificationApi() {
            override suspend fun getStatus() = StatusResult.Enabled
            override suspend fun requestEnableNotifications(): EnableNotificationsResult {
                return EnableNotificationsResult.Enabled
            }
            override suspend fun disableNotifications(): DisableNotificationsResult {
                return DisableNotificationsResult.Disabled
            }
        }
        val context = ApplicationProvider.getApplicationContext<Application>()
        val sharedPrefs = context.getSharedPreferences("repository_test", 0)
        val statusCache = StatusCache(sharedPrefs)

        val repository = createRepository(
            context = context,
            api = api,
            preferences = sharedPrefs,
            statusCache = statusCache,
            cdnService = object : CdnService {
                override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                    throw NotImplementedError()
                }

                override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest =
                    Manifest(listOf(), "risk", "config")

                override suspend fun getRiskCalculationParameters(
                    id: String,
                    cacheStrategy: CacheStrategy?
                ): RiskCalculationParameters {
                    throw NotImplementedError()
                }

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
            scheduler = object : BackgroundWorkScheduler {
                override fun schedule(intervalMinutes: Int) {
                }

                override fun cancel() {
                    throw AssertionError()
                }
            }
        )

        (context.getSystemService(BluetoothManager::class.java) as BluetoothManager).adapter.disable()
        shadowOf(context.getSystemService(LocationManager::class.java) as LocationManager).apply {
            setLocationEnabled(true)
            setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        }

        val result = async { repository.getStatus().take(2).toList() }
        yield()

        repository.requestEnableNotificationsForcingConsent()

        assertEquals(
            listOf(
                StatusResult.Disabled,
                StatusResult.BluetoothDisabled
            ),
            result.await()
        )
    }

    @Test
    fun `requestEnableNotifications updates statusCache to LocationPreconditionNotSatisfied when location is disabled`() = runBlockingTest {
        val api = object : FakeExposureNotificationApi() {
            override suspend fun getStatus() = StatusResult.Enabled
            override suspend fun requestEnableNotifications(): EnableNotificationsResult {
                return EnableNotificationsResult.Enabled
            }
            override suspend fun disableNotifications(): DisableNotificationsResult {
                return DisableNotificationsResult.Disabled
            }
        }
        val context = ApplicationProvider.getApplicationContext<Application>()
        val sharedPrefs = context.getSharedPreferences("repository_test", 0)
        val statusCache = StatusCache(sharedPrefs)

        val repository = createRepository(
            context = context,
            api = api,
            preferences = sharedPrefs,
            statusCache = statusCache,
            cdnService = object : CdnService {
                override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                    throw NotImplementedError()
                }

                override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest =
                    Manifest(listOf(), "risk", "config")

                override suspend fun getRiskCalculationParameters(
                    id: String,
                    cacheStrategy: CacheStrategy?
                ): RiskCalculationParameters {
                    throw NotImplementedError()
                }

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
            scheduler = object : BackgroundWorkScheduler {
                override fun schedule(intervalMinutes: Int) {
                }

                override fun cancel() {
                    throw AssertionError()
                }
            }
        )

        shadowOf(context.getSystemService(LocationManager::class.java) as LocationManager).apply {
            setLocationEnabled(false)
            setProviderEnabled(LocationManager.NETWORK_PROVIDER, false)
        }
        (context.getSystemService(BluetoothManager::class.java) as BluetoothManager).adapter.enable()

        val result = async { repository.getStatus().take(2).toList() }
        yield()

        repository.requestEnableNotificationsForcingConsent()

        val results = result.await()
        assertEquals(
            listOf(
                StatusResult.Disabled,
                StatusResult.LocationPreconditionNotSatisfied
            ),
            results
        )
    }

    @Test
    fun `requestEnableNotifications updates statusCache to Enabled when preconditions are correct`() = runBlockingTest {
        val api = object : FakeExposureNotificationApi() {
            override suspend fun getStatus() = StatusResult.Enabled
            override suspend fun requestEnableNotifications(): EnableNotificationsResult {
                return EnableNotificationsResult.Enabled
            }
            override suspend fun disableNotifications(): DisableNotificationsResult {
                return DisableNotificationsResult.Disabled
            }
        }
        val context = ApplicationProvider.getApplicationContext<Application>()
        val sharedPrefs = context.getSharedPreferences("repository_test", 0)
        val statusCache = StatusCache(sharedPrefs)

        val repository = createRepository(
            context = context,
            api = api,
            preferences = sharedPrefs,
            statusCache = statusCache,
            cdnService = object : CdnService {
                override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                    throw NotImplementedError()
                }

                override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest =
                    Manifest(listOf(), "risk", "config")

                override suspend fun getRiskCalculationParameters(
                    id: String,
                    cacheStrategy: CacheStrategy?
                ): RiskCalculationParameters {
                    throw NotImplementedError()
                }

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
            scheduler = object : BackgroundWorkScheduler {
                override fun schedule(intervalMinutes: Int) {
                }

                override fun cancel() {
                    throw AssertionError()
                }
            }
        )

        (context.getSystemService(BluetoothManager::class.java) as BluetoothManager).adapter.enable()
        shadowOf(context.getSystemService(LocationManager::class.java) as LocationManager).apply {
            setLocationEnabled(true)
            setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        }

        val result = async { repository.getStatus().take(2).toList() }
        yield()

        repository.requestEnableNotificationsForcingConsent()

        assertEquals(
            listOf(
                StatusResult.Disabled,
                StatusResult.Enabled
            ),
            result.await()
        )
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

            val repository = createRepository(
                api = api,
                cdnService = object : CdnService {
                    override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                        throw NotImplementedError()
                    }

                    override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest =
                        Manifest(listOf(), "risk", "config")

                    override suspend fun getRiskCalculationParameters(
                        id: String,
                        cacheStrategy: CacheStrategy?
                    ): RiskCalculationParameters {
                        throw NotImplementedError()
                    }

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
                scheduler = object : BackgroundWorkScheduler {
                    override fun schedule(intervalMinutes: Int) {
                    }

                    override fun cancel() {
                        throw AssertionError()
                    }
                }
            )

            repository.requestEnableNotificationsForcingConsent()
            assertTrue(disableCalled.get())
            assertTrue(enableCalled.get())
        }

    @Test
    fun `getLastExposureDate returns null when no exposure is reported`() = runBlocking {
        val repository = createRepository()
        assertNull(repository.getLastExposureDate().first())
    }

    @Test
    fun `rescheduleJobs schedules jobs when previously scheduled`() = runBlocking {
        val preferences = context.getSharedPreferences("repository_test", 0)
        preferences.edit {
            putLong("notifications_enabled_timestamp", 0)
        }

        val cancelled = AtomicBoolean(false)
        val scheduled = AtomicBoolean(false)

        val scheduler: BackgroundWorkScheduler = object : BackgroundWorkScheduler {
            override fun schedule(intervalMinutes: Int) {
                scheduled.set(true)
            }

            override fun cancel() {
                cancelled.set(true)
            }
        }
        val appConfigManager = AppConfigManager(object : CdnService {
            override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                throw IllegalStateException()
            }

            override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest = Manifest(
                emptyList(), "", "appconfig"
            )

            override suspend fun getRiskCalculationParameters(
                id: String,
                cacheStrategy: CacheStrategy?
            ): RiskCalculationParameters {
                throw IllegalArgumentException()
            }

            override suspend fun getAppConfig(
                id: String,
                cacheStrategy: CacheStrategy?
            ): AppConfig = AppConfig(updatePeriodMinutes = 10)

            override suspend fun getResourceBundle(
                id: String,
                cacheStrategy: CacheStrategy?
            ): ResourceBundle {
                throw IllegalStateException()
            }
        })
        val repository = createRepository(
            preferences = preferences,
            scheduler = scheduler,
            appConfigManager = appConfigManager
        )

        repository.rescheduleBackgroundJobs()

        assertTrue(cancelled.get())
        assertTrue(scheduled.get())
    }

    @Test
    fun `rescheduleJobs skips scheduling jobs when not previously scheduled`() = runBlocking {
        val preferences = context.getSharedPreferences("repository_test", 0)

        val cancelled = AtomicBoolean(false)
        val scheduled = AtomicBoolean(false)

        val scheduler: BackgroundWorkScheduler = object : BackgroundWorkScheduler {
            override fun schedule(intervalMinutes: Int) {
                scheduled.set(true)
            }

            override fun cancel() {
                cancelled.set(true)
            }
        }
        val repository = createRepository(preferences = preferences, scheduler = scheduler)

        repository.rescheduleBackgroundJobs()

        assertFalse(cancelled.get())
        assertFalse(scheduled.get())
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
            context,
            preferences,
            mock()
        ) {},
        clock: Clock = Clock.systemDefaultZone(),
        lifecycleOwner: LifecycleOwner = TestLifecycleOwner(Lifecycle.State.STARTED),
        signatureValidation: Boolean = false,
        signatureValidator: ResponseSignatureValidator = ResponseSignatureValidator(),
        scheduler: BackgroundWorkScheduler = fakeScheduler,
        appConfigManager: AppConfigManager = AppConfigManager(cdnService)
    ): ExposureNotificationsRepository {
        return ExposureNotificationsRepository(
            context,
            api,
            cdnService,
            AsyncSharedPreferences { preferences },
            scheduler,
            appLifecycleManager,
            statusCache,
            appConfigManager,
            clock,
            lifecycleOwner = lifecycleOwner,
            signatureValidation = signatureValidation,
            signatureValidator = signatureValidator
        )
    }

    private fun createTrustManager() = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        // root key is on the device
        keyStore.load(
            ExposureNotificationsRepositoryTest::class.java.getResourceAsStream("/nl-root.jks"),
            "test".toCharArray()
        )
        init(keyStore)
    }.trustManagers[0] as X509TrustManager
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
