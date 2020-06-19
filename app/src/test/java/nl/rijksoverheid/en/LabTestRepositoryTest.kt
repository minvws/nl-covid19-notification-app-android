/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import android.app.Application
import android.app.PendingIntent
import android.os.Build
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.en.api.LabTestService
import nl.rijksoverheid.en.enapi.TemporaryExposureKeysResult
import nl.rijksoverheid.en.labtest.LabTestRepository
import nl.rijksoverheid.en.labtest.RegistrationResult
import nl.rijksoverheid.en.labtest.UploadScheduler
import nl.rijksoverheid.en.test.FakeExposureNotificationApi
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

private val NOOP_SCHEDULER: UploadScheduler = {}

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.O_MR1])
class LabTestRepositoryTest {
    private lateinit var mockWebServer: MockWebServer
    private val instant = Instant.parse("2020-06-20T10:15:30.00Z")
    private val clock = Clock.fixed(instant, ZoneId.of("UTC"))

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
                lazy { prefs },
                FakeExposureNotificationApi(),
                LabTestService.create(
                    ApplicationProvider.getApplicationContext(),
                    baseUrl = mockWebServer.url("/").toString()
                ),
                NOOP_SCHEDULER,
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
            lazy { prefs },
            FakeExposureNotificationApi(),
            LabTestService.create(
                ApplicationProvider.getApplicationContext(),
                baseUrl = mockWebServer.url("/").toString()
            ),
            NOOP_SCHEDULER,
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
                lazy { prefs },
                FakeExposureNotificationApi(),
                LabTestService.create(
                    ApplicationProvider.getApplicationContext(),
                    baseUrl = mockWebServer.url("/").toString()
                ),
                NOOP_SCHEDULER,
                clock
            )

            val result = repository.registerForUpload()

            assertEquals(1, mockWebServer.requestCount)
            assertTrue(result is RegistrationResult.Success)
            assertEquals("server-code", (result as RegistrationResult.Success).code)
        }

    @Test
    fun `uploadDiagnosticKeysOrDecoy without pending upload does not make a request and returns Completed`() =
        runBlocking {
            mockWebServer.start()
            val prefs =
                ApplicationProvider.getApplicationContext<Application>()
                    .getSharedPreferences("test", 0)

            val repository = LabTestRepository(
                lazy { prefs },
                FakeExposureNotificationApi(),
                LabTestService.create(
                    ApplicationProvider.getApplicationContext(),
                    baseUrl = mockWebServer.url("/").toString()
                ),
                NOOP_SCHEDULER,
                clock
            )

            val result = repository.uploadDiagnosticKeysOrDecoy()

            assertEquals(0, mockWebServer.requestCount)
            assertEquals(LabTestRepository.UploadDiagnosticKeysResult.Completed, result)
        }

    @Test
    fun `uploadDiagnosticKeys uploads initial keys and returns Initial`() = runBlocking {
        mockWebServer.enqueue(MockResponse())
        mockWebServer.start()
        val prefs =
            ApplicationProvider.getApplicationContext<Application>().getSharedPreferences("test", 0)

        prefs.edit {
            putBoolean("upload_diagnostic_keys", true)
            putString("bucket_id", "bucket-id")
            putString("confirmation_key", "confirmation-key")
        }

        // yesterdays interval
        val startInterval = ((((clock.millis() / 1000L) / 600) / 144) * 144) - 144
        val repository = LabTestRepository(
            lazy { prefs },
            object : FakeExposureNotificationApi() {
                override suspend fun requestTemporaryExposureKeyHistory(): TemporaryExposureKeysResult {
                    return TemporaryExposureKeysResult.Success(
                        listOf(
                            TemporaryExposureKey.TemporaryExposureKeyBuilder().setKeyData(
                                ByteArray(16)
                            ).setRollingPeriod(144)
                                .setRollingStartIntervalNumber(startInterval.toInt())
                                .build()
                        )
                    )
                }
            },
            LabTestService.create(
                ApplicationProvider.getApplicationContext(),
                baseUrl = mockWebServer.url("/").toString()
            ),
            NOOP_SCHEDULER,
            clock
        )

        val result = repository.uploadDiagnosticKeysOrDecoy()

        assertEquals(1, mockWebServer.requestCount)
        val expectedDate = LocalDateTime.ofInstant(
            Instant.ofEpochMilli((startInterval + 144) * 600 * 1000L),
            clock.zone
        )
        assertEquals(LabTestRepository.UploadDiagnosticKeysResult.Initial(expectedDate), result)
    }

    @Test
    fun `uploadDiagnosticKeys with server error returns Retry`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        mockWebServer.start()
        val prefs =
            ApplicationProvider.getApplicationContext<Application>().getSharedPreferences("test", 0)

        prefs.edit {
            putBoolean("upload_diagnostic_keys", true)
            putString("bucket_id", "bucket-id")
            putString("confirmation_key", "confirmation-key")
        }

        // yesterdays interval
        val startInterval = ((((clock.millis() / 1000L) / 600) / 144) * 144) - 144
        val repository = LabTestRepository(
            lazy { prefs },
            object : FakeExposureNotificationApi() {
                override suspend fun requestTemporaryExposureKeyHistory(): TemporaryExposureKeysResult {
                    return TemporaryExposureKeysResult.Success(
                        listOf(
                            TemporaryExposureKey.TemporaryExposureKeyBuilder().setKeyData(
                                ByteArray(16)
                            ).setRollingPeriod(144)
                                .setRollingStartIntervalNumber(startInterval.toInt())
                                .build()
                        )
                    )
                }
            },
            LabTestService.create(
                ApplicationProvider.getApplicationContext(),
                baseUrl = mockWebServer.url("/").toString()
            ),
            NOOP_SCHEDULER,
            clock
        )

        val result = repository.uploadDiagnosticKeysOrDecoy()

        assertEquals(1, mockWebServer.requestCount)
        assertEquals(LabTestRepository.UploadDiagnosticKeysResult.Retry, result)
    }

    @Test
    fun `uploadDiagnosticKeys with previously uploaded keys returns Initial`() = runBlocking {
        mockWebServer.enqueue(MockResponse())
        mockWebServer.start()
        val prefs =
            ApplicationProvider.getApplicationContext<Application>().getSharedPreferences("test", 0)

        // yesterdays interval
        val startInterval = ((((clock.millis() / 1000L) / 600) / 144) * 144) - 144

        prefs.edit {
            putBoolean("upload_diagnostic_keys", true)
            putString("bucket_id", "bucket-id")
            putString("confirmation_key", "confirmation-key")
            putLong("upload_diagnostic_key_start_interval", startInterval)
        }

        val repository = LabTestRepository(
            lazy { prefs },
            object : FakeExposureNotificationApi() {
                override suspend fun requestTemporaryExposureKeyHistory(): TemporaryExposureKeysResult {
                    return TemporaryExposureKeysResult.Success(
                        listOf(
                            TemporaryExposureKey.TemporaryExposureKeyBuilder().setKeyData(
                                ByteArray(16)
                            ).setRollingPeriod(144)
                                .setRollingStartIntervalNumber(startInterval.toInt())
                                .build()
                        )
                    )
                }
            },
            LabTestService.create(
                ApplicationProvider.getApplicationContext(),
                baseUrl = mockWebServer.url("/").toString()
            ),
            NOOP_SCHEDULER,
            clock
        )

        val result = repository.uploadDiagnosticKeysOrDecoy()

        assertEquals(0, mockWebServer.requestCount)
        val expectedDate = LocalDateTime.ofInstant(
            Instant.ofEpochMilli((startInterval + 144) * 600 * 1000L),
            clock.zone
        )
        assertEquals(LabTestRepository.UploadDiagnosticKeysResult.Initial(expectedDate), result)
    }

    @Test
    fun `uploadDiagnosticKeys with previously uploaded keys uploads final key and returns Completed`() =
        runBlocking {
            mockWebServer.enqueue(MockResponse())
            mockWebServer.start()
            val prefs =
                ApplicationProvider.getApplicationContext<Application>()
                    .getSharedPreferences("test", 0)

            // yesterdays interval
            val startInterval = ((((clock.millis() / 1000L) / 600) / 144) * 144) - 144

            prefs.edit {
                putBoolean("upload_diagnostic_keys", true)
                putString("bucket_id", "bucket-id")
                putString("confirmation_key", "confirmation-key")
                putLong("upload_diagnostic_key_start_interval", startInterval)
            }

            val repository = LabTestRepository(
                lazy { prefs },
                object : FakeExposureNotificationApi() {
                    override suspend fun requestTemporaryExposureKeyHistory(): TemporaryExposureKeysResult {
                        return TemporaryExposureKeysResult.Success(
                            listOf(
                                TemporaryExposureKey.TemporaryExposureKeyBuilder().setKeyData(
                                    ByteArray(16)
                                ).setRollingPeriod(144)
                                    .setRollingStartIntervalNumber(startInterval.toInt() + 144)
                                    .build()
                            )
                        )
                    }
                },
                LabTestService.create(
                    ApplicationProvider.getApplicationContext(),
                    baseUrl = mockWebServer.url("/").toString()
                ),
                NOOP_SCHEDULER,
                clock
            )

            val result = repository.uploadDiagnosticKeysOrDecoy()

            assertEquals(1, mockWebServer.requestCount)
            assertEquals(LabTestRepository.UploadDiagnosticKeysResult.Completed, result)
        }

    @Test
    fun `uploadDiagnosticKeys with expired user consent returns Completed`() = runBlocking {
        mockWebServer.enqueue(MockResponse())
        mockWebServer.start()
        val prefs =
            ApplicationProvider.getApplicationContext<Application>().getSharedPreferences("test", 0)

        prefs.edit {
            putBoolean("upload_diagnostic_keys", true)
            putString("bucket_id", "bucket-id")
            putString("confirmation_key", "confirmation-key")
        }

        val repository = LabTestRepository(
            lazy { prefs },
            object : FakeExposureNotificationApi() {
                override suspend fun requestTemporaryExposureKeyHistory(): TemporaryExposureKeysResult {
                    return TemporaryExposureKeysResult.RequireConsent(
                        Shadow.newInstanceOf(
                            PendingIntent::class.java
                        )
                    )
                }
            },
            LabTestService.create(
                ApplicationProvider.getApplicationContext(),
                baseUrl = mockWebServer.url("/").toString()
            ),
            NOOP_SCHEDULER,
            clock
        )

        val result = repository.uploadDiagnosticKeysOrDecoy()

        assertEquals(0, mockWebServer.requestCount)
        assertEquals(LabTestRepository.UploadDiagnosticKeysResult.Completed, result)
        assertFalse(prefs.contains("upload_diagnostic_keys"))
        assertFalse(prefs.contains("bucket_id"))
        assertFalse(prefs.contains("confirmation_key"))
    }
}
