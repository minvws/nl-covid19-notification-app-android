/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import android.app.Application
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.en.api.CacheStrategy
import nl.rijksoverheid.en.api.CdnService
import nl.rijksoverheid.en.api.HmacSecret
import nl.rijksoverheid.en.api.LabTestService
import nl.rijksoverheid.en.api.RequestSize
import nl.rijksoverheid.en.api.model.AppConfig
import nl.rijksoverheid.en.api.model.Manifest
import nl.rijksoverheid.en.api.model.PostKeysRequest
import nl.rijksoverheid.en.api.model.Registration
import nl.rijksoverheid.en.api.model.RegistrationRequest
import nl.rijksoverheid.en.api.model.ResourceBundle
import nl.rijksoverheid.en.api.model.RiskCalculationParameters
import nl.rijksoverheid.en.config.AppConfigManager
import nl.rijksoverheid.en.enapi.StatusResult
import nl.rijksoverheid.en.labtest.DecoyScheduler
import nl.rijksoverheid.en.labtest.KeysStorage
import nl.rijksoverheid.en.labtest.LabTestRepository
import nl.rijksoverheid.en.labtest.RegistrationResult
import nl.rijksoverheid.en.labtest.UploadScheduler
import nl.rijksoverheid.en.preferences.AsyncSharedPreferences
import nl.rijksoverheid.en.test.FakeExposureNotificationApi
import okhttp3.ResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

private val NOOP_SCHEDULER: UploadScheduler = {}
private val NOOP_DECOY_SCHEDULER: DecoyScheduler = {}

@RunWith(RobolectricTestRunner::class)
class LabTestRepositoryTest {
    private lateinit var mockWebServer: MockWebServer
    private val instant = Instant.parse("2020-06-20T10:15:30.00Z")
    private val clock = Clock.fixed(instant, ZoneId.of("UTC"))

    private val cdnService = object : CdnService {
        override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
            throw NotImplementedError()
        }

        override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest =
            Manifest(listOf(), "", "appconfig")

        override suspend fun getRiskCalculationParameters(id: String): RiskCalculationParameters {
            throw NotImplementedError()
        }

        override suspend fun getAppConfig(id: String, cacheStrategy: CacheStrategy?): AppConfig {
            return AppConfig(requestMinimumSize = 0, requestMaximumSize = 0, decoyProbability = 1.0)
        }

        override suspend fun getResourceBundle(
            id: String,
            cacheStrategy: CacheStrategy?
        ): ResourceBundle {
            throw java.lang.IllegalStateException()
        }
    }

    private val appConfigManager =
        AppConfigManager(cdnService)

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `registerForUpload calls the register endpoint, caches and returns the code`() =
        runBlocking {
            mockWebServer.enqueue(MockResponse().setBody("{\"labConfirmationId\":\"user-code\",\"bucketId\":\"vPUC39ia6grsuAnpEEullKJTea6XBJC475EEKpZaD+I=\",\"confirmationKey\":\"I+dl3vS844SEZNYUZ1GDayU9yfGhN5oF0ae70q+Runk=\",\"validity\":64028}"))
            mockWebServer.start()

            val prefs =
                ApplicationProvider.getApplicationContext<Application>()
                    .getSharedPreferences("test", 0)
            val repository = LabTestRepository(
                AsyncSharedPreferences { prefs },
                FakeExposureNotificationApi(),
                LabTestService.create(
                    ApplicationProvider.getApplicationContext(),
                    BuildConfig.VERSION_CODE,
                    baseUrl = mockWebServer.url("/").toString()
                ),
                NOOP_SCHEDULER,
                NOOP_DECOY_SCHEDULER,
                appConfigManager,
                clock
            )

            val result = repository.registerForUpload()

            assertTrue(result is RegistrationResult.Success)
            assertEquals("user-code", (result as RegistrationResult.Success).code)
            assertEquals(
                instant.epochSecond + 64028,
                prefs.getLong("registration_expiration", 0) / 1000L
            )
        }

    @Test
    fun `registerForUpload with cached key returns the code`() = runBlocking {
        mockWebServer.start()

        val prefs =
            ApplicationProvider.getApplicationContext<Application>().getSharedPreferences("test", 0)

        prefs.edit {
            putString("lab_confirmation_id", "cached-code")
            putLong("registration_expiration", clock.millis() + 30000)
        }

        val repository = LabTestRepository(
            AsyncSharedPreferences { prefs },
            FakeExposureNotificationApi(),
            LabTestService.create(
                ApplicationProvider.getApplicationContext(),
                BuildConfig.VERSION_CODE,
                baseUrl = mockWebServer.url("/").toString()
            ),
            NOOP_SCHEDULER,
            NOOP_DECOY_SCHEDULER,
            appConfigManager,
            clock
        )

        val result = repository.registerForUpload()

        assertEquals(0, mockWebServer.requestCount)
        assertTrue(result is RegistrationResult.Success)
        assertEquals("cached-code", (result as RegistrationResult.Success).code)
    }

    @Test
    fun `registerForUpload with expired key requests a new key and returns the code`() =
        runBlocking {
            mockWebServer.enqueue(MockResponse().setBody("{\"labConfirmationId\":\"server-code\",\"bucketId\":\"vPUC39ia6grsuAnpEEullKJTea6XBJC475EEKpZaD+I=\",\"confirmationKey\":\"I+dl3vS844SEZNYUZ1GDayU9yfGhN5oF0ae70q+Runk=\",\"validity\":64028}"))
            mockWebServer.start()

            val prefs =
                ApplicationProvider.getApplicationContext<Application>()
                    .getSharedPreferences("test", 0)

            prefs.edit {
                putString("lab_confirmation_id", "cached-code")
                putLong("registration_expiration", clock.millis() - 30000)
            }

            val repository = LabTestRepository(
                AsyncSharedPreferences { prefs },
                FakeExposureNotificationApi(),
                LabTestService.create(
                    ApplicationProvider.getApplicationContext(),
                    BuildConfig.VERSION_CODE,
                    baseUrl = mockWebServer.url("/").toString()
                ),
                NOOP_SCHEDULER,
                NOOP_DECOY_SCHEDULER,
                appConfigManager,
                clock
            )

            val result = repository.registerForUpload()

            assertEquals(1, mockWebServer.requestCount)
            assertTrue(result is RegistrationResult.Success)
            assertEquals("server-code", (result as RegistrationResult.Success).code)
        }

    @Test
    fun `registerForUpload with invalid response propagates RegistrationResult UnknownError`() =
        runBlocking {
            mockWebServer.enqueue(MockResponse().setBody("{\"labConfirmationId\":,\"bucketId\":,\"confirmationKey\":,\"validity\":}"))
            mockWebServer.start()

            val prefs =
                ApplicationProvider.getApplicationContext<Application>()
                    .getSharedPreferences("test", 0)

            val repository = LabTestRepository(
                AsyncSharedPreferences { prefs },
                FakeExposureNotificationApi(),
                LabTestService.create(
                    ApplicationProvider.getApplicationContext(),
                    BuildConfig.VERSION_CODE,
                    baseUrl = mockWebServer.url("/").toString()
                ),
                NOOP_SCHEDULER,
                NOOP_DECOY_SCHEDULER,
                appConfigManager,
                clock
            )

            val result = repository.registerForUpload()

            assertEquals(1, mockWebServer.requestCount)
            assertTrue(result is RegistrationResult.UnknownError)
        }

    @Test
    fun `uploadDiagnosticKeysIfPending without pending upload does not make a request and returns Completed`() =
        runBlocking {
            mockWebServer.start()
            val prefs =
                ApplicationProvider.getApplicationContext<Application>()
                    .getSharedPreferences("test", 0)

            val repository = LabTestRepository(
                AsyncSharedPreferences { prefs },
                FakeExposureNotificationApi(),
                LabTestService.create(
                    ApplicationProvider.getApplicationContext(),
                    BuildConfig.VERSION_CODE,
                    baseUrl = mockWebServer.url("/").toString()
                ),
                NOOP_SCHEDULER,
                NOOP_DECOY_SCHEDULER,
                appConfigManager,
                clock
            )

            val result = repository.uploadDiagnosticKeysIfPending()

            assertEquals(0, mockWebServer.requestCount)
            assertEquals(LabTestRepository.UploadDiagnosticKeysResult.Success, result)
        }

    @Test
    fun `uploadDiagnosticKeysIfPending with expired upload returns Expired`() =
        runBlocking {
            mockWebServer.start()
            val prefs =
                ApplicationProvider.getApplicationContext<Application>()
                    .getSharedPreferences("test", 0)

            prefs.edit {
                putBoolean("upload_diagnostic_keys", true)
                putString("bucket_id", "bucket-id")
                putString("confirmation_key", "confirmation-key")
                putLong("registration_expiration", clock.millis() - 30000L)
            }

            val repository = LabTestRepository(
                AsyncSharedPreferences { prefs },
                FakeExposureNotificationApi(),
                LabTestService.create(
                    ApplicationProvider.getApplicationContext(),
                    BuildConfig.VERSION_CODE,
                    baseUrl = mockWebServer.url("/").toString()
                ),
                NOOP_SCHEDULER,
                NOOP_DECOY_SCHEDULER,
                appConfigManager,
                clock
            )

            val result = repository.uploadDiagnosticKeysIfPending()

            assertEquals(0, mockWebServer.requestCount)
            assertEquals(LabTestRepository.UploadDiagnosticKeysResult.Expired, result)
        }

    @Test
    fun `uploadDiagnosticKeysIfPending with a previous upload and expired registration returns Success`() =
        runBlocking {
            mockWebServer.start()
            val prefs =
                ApplicationProvider.getApplicationContext<Application>()
                    .getSharedPreferences("test", 0)

            prefs.edit {
                putBoolean("upload_diagnostic_keys", true)
                putBoolean("upload_completed", true)
                putString("bucket_id", "bucket-id")
                putString("confirmation_key", "confirmation-key")
                putLong("registration_expiration", clock.millis() - 30000L)
            }

            val repository = LabTestRepository(
                AsyncSharedPreferences { prefs },
                FakeExposureNotificationApi(),
                LabTestService.create(
                    ApplicationProvider.getApplicationContext(),
                    BuildConfig.VERSION_CODE,
                    baseUrl = mockWebServer.url("/").toString()
                ),
                NOOP_SCHEDULER,
                NOOP_DECOY_SCHEDULER,
                appConfigManager,
                clock
            )

            val result = repository.uploadDiagnosticKeysIfPending()

            assertEquals(0, mockWebServer.requestCount)
            assertEquals(LabTestRepository.UploadDiagnosticKeysResult.Success, result)
        }

    @Test
    fun `uploadDiagnosticKeysIfPending uploads keys and resets state`() = runBlocking {
        mockWebServer.enqueue(MockResponse())
        mockWebServer.start()
        val prefs =
            ApplicationProvider.getApplicationContext<Application>().getSharedPreferences("test", 0)

        val keyStorage = KeysStorage("upload_pending_keys", prefs)

        keyStorage.storeKeys(
            listOf(
                TemporaryExposureKey.TemporaryExposureKeyBuilder().setKeyData(ByteArray(16)).build()
            )
        )

        prefs.edit {
            putBoolean("upload_diagnostic_keys", true)
            putString("bucket_id", "bucket-id")
            putString("confirmation_key", "confirmation-key")
            putLong("registration_expiration", clock.millis() + 60000L)
        }

        val repository = LabTestRepository(
            AsyncSharedPreferences { prefs },
            mock(),
            LabTestService.create(
                ApplicationProvider.getApplicationContext(),
                BuildConfig.VERSION_CODE,
                baseUrl = mockWebServer.url("/").toString()
            ),
            NOOP_SCHEDULER,
            NOOP_DECOY_SCHEDULER,
            appConfigManager,
            clock
        )

        val result = repository.uploadDiagnosticKeysIfPending()

        assertEquals(1, mockWebServer.requestCount)
        assertTrue(keyStorage.getKeys().isEmpty())
        assertEquals(LabTestRepository.UploadDiagnosticKeysResult.Success, result)
    }

    @Test
    fun `uploadDiagnosticKeys with server error returns Retry`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        mockWebServer.start()
        val prefs =
            ApplicationProvider.getApplicationContext<Application>().getSharedPreferences("test", 0)

        val keyStorage = KeysStorage("upload_pending_keys", prefs)
        keyStorage.storeKeys(
            listOf(
                TemporaryExposureKey.TemporaryExposureKeyBuilder().setKeyData(ByteArray(16)).build()
            )
        )

        prefs.edit {
            putBoolean("upload_diagnostic_keys", true)
            putString("bucket_id", "bucket-id")
            putString("confirmation_key", "confirmation-key")
            putLong("registration_expiration", clock.millis() + 60000L)
        }

        val repository = LabTestRepository(
            AsyncSharedPreferences { prefs },
            mock(),
            LabTestService.create(
                ApplicationProvider.getApplicationContext(),
                BuildConfig.VERSION_CODE,
                baseUrl = mockWebServer.url("/").toString()
            ),
            NOOP_SCHEDULER,
            NOOP_DECOY_SCHEDULER,
            appConfigManager,
            clock
        )

        val result = repository.uploadDiagnosticKeysIfPending()

        assertEquals(1, mockWebServer.requestCount)
        assertEquals(LabTestRepository.UploadDiagnosticKeysResult.Retry, result)
    }

    @Test
    fun `scheduleNextDecoyScheduleSequence schedules a decoy for the current day`() =
        runBlocking {
            val delay = AtomicLong(0)
            // current time after the schedule window
            val instant = Instant.parse("2020-06-20T05:00:30.00Z")
            val clock = Clock.fixed(instant, ZoneId.of("UTC"))
            val decoyTime =
                LocalDate.now(clock).atTime(7, 30).atZone(clock.zone).toInstant().toEpochMilli()

            val random = mock<Random> {
                on { nextDouble() }.thenReturn(1.0)
                on { nextLong(any(), any()) }.thenReturn(decoyTime)
            }

            val repository = LabTestRepository(
                AsyncSharedPreferences {
                    ApplicationProvider.getApplicationContext<Application>()
                        .getSharedPreferences("test", 0)
                },
                object : FakeExposureNotificationApi() {
                    override suspend fun getStatus(): StatusResult = StatusResult.Enabled
                },
                mock(),
                NOOP_SCHEDULER,
                { delayMillis -> delay.set(delayMillis) },
                appConfigManager,
                clock,
                random
            )

            repository.scheduleNextDecoyScheduleSequence()

            assertTrue(delay.get() > 0)
            val date = LocalDateTime.now(clock).plus(delay.get(), ChronoUnit.MILLIS)
            assertEquals(LocalDate.now(clock).atTime(7, 30), date)
        }

    @Test
    fun `scheduleNextDecoyScheduleSequence schedules the next day when scheduled time has passed`() =
        runBlocking {
            val delay = AtomicLong(0)
            // current time after the schedule window
            val instant = Instant.parse("2020-06-20T20:15:30.00Z")
            val clock = Clock.fixed(instant, ZoneId.of("UTC"))
            val decoyTime =
                LocalDate.now(clock).atTime(7, 0).atZone(clock.zone).toInstant().toEpochMilli()

            val random = mock<Random> {
                on { nextDouble() }.thenReturn(1.0)
                on { nextLong(any(), any()) }.thenReturn(decoyTime)
            }

            val repository = LabTestRepository(
                AsyncSharedPreferences {
                    ApplicationProvider.getApplicationContext<Application>()
                        .getSharedPreferences("test", 0)
                },
                mock(),
                mock(),
                NOOP_SCHEDULER,
                { delayMillis -> delay.set(delayMillis) },
                appConfigManager,
                clock,
                random
            )

            repository.scheduleNextDecoyScheduleSequence()

            assertTrue(delay.get() > 0)
            val date = LocalDateTime.now(clock).plus(delay.get(), ChronoUnit.MILLIS)
            assertEquals(LocalDate.now(clock).plusDays(1).atTime(7, 0), date)
        }

    @Test
    fun `sendDecoyTraffic calls register if registration is not valid`() = runBlocking {
        val registrationCalled = AtomicBoolean(false)
        var postedRequest: PostKeysRequest? = null

        val api = object : LabTestService {
            override suspend fun register(
                request: RegistrationRequest,
                sizes: RequestSize
            ): Registration {
                registrationCalled.set(true)
                return Registration("12345", "122345", byteArrayOf(1, 2, 3, 4, 5), 3600000)
            }

            override suspend fun postKeys(
                request: PostKeysRequest,
                hmacSecret: HmacSecret,
                requestSize: RequestSize
            ) {
                throw IllegalStateException()
            }

            override suspend fun stopKeys(
                request: PostKeysRequest,
                hmacSecret: HmacSecret,
                requestSize: RequestSize
            ) {
                postedRequest = request
            }
        }

        val random = mock<Random> {
            on { nextInt(any(), any()) }.thenReturn(0)
            on { nextLong(any(), any()) }.thenReturn(1000)
        }

        val repository = LabTestRepository(
            AsyncSharedPreferences {
                ApplicationProvider.getApplicationContext<Application>()
                    .getSharedPreferences("test", 0)
            },
            object : FakeExposureNotificationApi() {
                override suspend fun getStatus(): StatusResult = StatusResult.Enabled
            },
            api,
            NOOP_SCHEDULER,
            NOOP_DECOY_SCHEDULER,
            appConfigManager,
            clock,
            random = random
        )

        val result = repository.sendDecoyTraffic()
        assertEquals(LabTestRepository.SendDecoyResult.Registered(1000), result)
        assertTrue(registrationCalled.get())
        assertNull(postedRequest)
    }

    @Test
    fun `sendDecoyTraffic skips register if registration is valid`() = runBlocking {
        var postedRequest: PostKeysRequest? = null

        val prefs =
            ApplicationProvider.getApplicationContext<Application>().getSharedPreferences("test", 0)

        prefs.edit {
            putString("lab_confirmation_id", "cached-code")
            putLong("registration_expiration", clock.millis() + 30000)
            putString("bucket_id", "bucket-id")
        }

        val api = object : LabTestService {
            override suspend fun register(
                request: RegistrationRequest,
                sizes: RequestSize
            ): Registration {
                throw AssertionError()
            }

            override suspend fun postKeys(
                request: PostKeysRequest,
                hmacSecret: HmacSecret,
                requestSize: RequestSize
            ) {
                throw IllegalStateException()
            }

            override suspend fun stopKeys(
                request: PostKeysRequest,
                hmacSecret: HmacSecret,
                requestSize: RequestSize
            ) {
                postedRequest = request
            }
        }

        val repository = LabTestRepository(
            AsyncSharedPreferences {
                prefs
            },
            object : FakeExposureNotificationApi() {
                override suspend fun getStatus(): StatusResult = StatusResult.Enabled
            },
            api,
            NOOP_SCHEDULER,
            NOOP_DECOY_SCHEDULER,
            appConfigManager,
            clock
        )

        val result = repository.sendDecoyTraffic()
        assertEquals(LabTestRepository.SendDecoyResult.Success, result)
        assertNotNull(postedRequest)
    }

    @Test
    fun `sendDecoyTraffic skips if exposure notifications are disabled`() = runBlocking {
        val prefs =
            ApplicationProvider.getApplicationContext<Application>().getSharedPreferences("test", 0)

        prefs.edit {
            putString("lab_confirmation_id", "cached-code")
            putLong("registration_expiration", clock.millis() + 30000)
            putString("bucket_id", "bucket-id")
        }

        val api = object : LabTestService {
            override suspend fun register(
                request: RegistrationRequest,
                sizes: RequestSize
            ): Registration {
                throw AssertionError()
            }

            override suspend fun postKeys(
                request: PostKeysRequest,
                hmacSecret: HmacSecret,
                requestSize: RequestSize
            ) {
                throw AssertionError()
            }

            override suspend fun stopKeys(
                request: PostKeysRequest,
                hmacSecret: HmacSecret,
                requestSize: RequestSize
            ) {
                throw AssertionError()
            }
        }

        val repository = LabTestRepository(
            AsyncSharedPreferences {
                prefs
            },
            object : FakeExposureNotificationApi() {
                override suspend fun getStatus(): StatusResult {
                    return StatusResult.Disabled
                }
            },
            api,
            NOOP_SCHEDULER,
            NOOP_DECOY_SCHEDULER,
            appConfigManager,
            clock
        )

        val result = repository.sendDecoyTraffic()
        assertEquals(LabTestRepository.SendDecoyResult.Success, result)
    }
}
