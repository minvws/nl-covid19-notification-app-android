/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.enapi.nearby

import android.app.PendingIntent
import android.os.Build
import com.google.android.gms.common.api.Api
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.ApiKey
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.en.enapi.DiagnosisKeysResult
import nl.rijksoverheid.en.enapi.DisableNotificationsResult
import nl.rijksoverheid.en.enapi.EnableNotificationsResult
import nl.rijksoverheid.en.enapi.NearbyExposureNotificationApi
import nl.rijksoverheid.en.enapi.StatusResult
import nl.rijksoverheid.en.enapi.TemporaryExposureKeysResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.O_MR1])
class NearbyExposureNotificationApiTest {
    @Test
    fun `getStatus with enabled api returns enabled status`() = runBlocking {
        // GIVEN
        val api =
            NearbyExposureNotificationApi(object :
                FakeExposureNotificationsClient() {
                override fun isEnabled(): Task<Boolean> = Tasks.forResult(true)
            })

        // WHEN
        val status = api.getStatus()

        // THEN
        assertEquals(StatusResult.Enabled, status)
    }

    @Test
    fun `getStatus with disabled api returns disabled status`() = runBlocking {
        // GIVEN
        val api =
            NearbyExposureNotificationApi(object :
                FakeExposureNotificationsClient() {
                override fun isEnabled(): Task<Boolean> = Tasks.forResult(false)
            })

        // WHEN
        val status = api.getStatus()

        // THEN
        assertEquals(StatusResult.Disabled, status)
    }

    @Test
    fun `getStatus with api not connected error returns unavailable status`() = runBlocking {
        // GIVEN
        val api =
            NearbyExposureNotificationApi(object :
                FakeExposureNotificationsClient() {
                override fun isEnabled(): Task<Boolean> =
                    Tasks.forException(ApiException(Status(CommonStatusCodes.API_NOT_CONNECTED)))
            })

        // WHEN
        val status = api.getStatus()

        // THEN
        assertTrue(status is StatusResult.Unavailable)
        assertEquals(
            CommonStatusCodes.API_NOT_CONNECTED,
            (status as StatusResult.Unavailable).statusCode
        )
    }

    @Test
    fun `getStatus with api not connected error returns most specific error code`() = runBlocking {
        // GIVEN
        // actual message as observed from the API
        val message =
            "API: Nearby.EXPOSURE_NOTIFICATION_API is not available on this device. Connection failed with: ConnectionResult{statusCode=UNKNOWN_ERROR_CODE(39503), resolution=null, message=null}"

        val api =
            NearbyExposureNotificationApi(object :
                FakeExposureNotificationsClient() {
                override fun isEnabled(): Task<Boolean> =
                    Tasks.forException(
                        ApiException(
                            Status(
                                CommonStatusCodes.API_NOT_CONNECTED,
                                message
                            )
                        )
                    )
            })

        // WHEN
        val status = api.getStatus()

        // THEN
        assertTrue(status is StatusResult.Unavailable)
        assertEquals(
            ExposureNotificationStatusCodes.FAILED_SERVICE_DISABLED,
            (status as StatusResult.Unavailable).statusCode
        )
    }

    @Test
    fun `enabled api unknown error returns the unknown error`() = runBlocking {
        // GIVEN
        val api =
            NearbyExposureNotificationApi(object :
                FakeExposureNotificationsClient() {
                override fun isEnabled(): Task<Boolean> =
                    Tasks.forException(ApiException(Status(CommonStatusCodes.INTERNAL_ERROR)))
            })

        // WHEN
        val status = api.getStatus()

        // THEN
        assertTrue(status is StatusResult.UnknownError)
        assertEquals(
            CommonStatusCodes.INTERNAL_ERROR,
            ((status as StatusResult.UnknownError).exception as ApiException).statusCode
        )
    }

    @Test
    fun `requestEnableNotifications without errors returns Enabled`() = runBlocking {
        // GIVEN
        val api =
            NearbyExposureNotificationApi(object :
                FakeExposureNotificationsClient() {
                override fun start(): Task<Void> = Tasks.forResult(null)
            })

        // WHEN
        val status = api.requestEnableNotifications()

        // THEN
        assertEquals(EnableNotificationsResult.Enabled, status)
    }

    @Test
    fun `requestEnableNotifications with errors returns UnknownError`() = runBlocking {
        // GIVEN
        val exception = RuntimeException("test")
        val api =
            NearbyExposureNotificationApi(object :
                FakeExposureNotificationsClient() {
                override fun start(): Task<Void> = Tasks.forException(exception)
            })

        // WHEN
        val status = api.requestEnableNotifications()

        // THEN
        assertTrue(status is EnableNotificationsResult.UnknownError)
        assertEquals((status as EnableNotificationsResult.UnknownError).exception, exception)
    }

    @Test
    fun `requestEnableNotifications without user consent returns ResolutionRequired`() =
        runBlocking {
            // GIVEN
            val pendingIntent = Shadow.newInstanceOf(PendingIntent::class.java)
            val api =
                NearbyExposureNotificationApi(object :
                    FakeExposureNotificationsClient() {
                    override fun start(): Task<Void> = Tasks.forException(
                        ApiException(
                            Status(
                                CommonStatusCodes.RESOLUTION_REQUIRED,
                                "Resolution required",
                                pendingIntent
                            )
                        )
                    )
                })

            // WHEN
            val status = api.requestEnableNotifications()

            // THEN
            assertTrue(status is EnableNotificationsResult.ResolutionRequired)
            // check that the pending intent is passed through
            assertSame(
                (status as EnableNotificationsResult.ResolutionRequired).resolution,
                pendingIntent
            )
        }

    @Test
    fun `disableNotifications without errors returns Disabled`() = runBlocking {
        // GIVEN
        val api =
            NearbyExposureNotificationApi(object :
                FakeExposureNotificationsClient() {
                override fun stop(): Task<Void> = Tasks.forResult(null)
            })

        // WHEN
        val status = api.disableNotifications()

        // THEN
        assertEquals(DisableNotificationsResult.Disabled, status)
    }

    @Test
    fun `disableNotifications with errors returns UnknownError`() = runBlocking {
        // GIVEN
        val exception = ApiException(Status.RESULT_INTERNAL_ERROR)
        val api =
            NearbyExposureNotificationApi(object :
                FakeExposureNotificationsClient() {
                override fun stop(): Task<Void> = Tasks.forException(exception)
            })

        // WHEN
        val status = api.disableNotifications()

        // THEN
        assertTrue(status is DisableNotificationsResult.UnknownError)
        assertSame((status as DisableNotificationsResult.UnknownError).exception, exception)
    }

    @Test
    fun `requestTemporaryExposureKeys without consent returns RequireConsent`() = runBlocking {
        // GIVEN
        val pendingIntent = Shadow.newInstanceOf(PendingIntent::class.java)
        val api =
            NearbyExposureNotificationApi(object :
                FakeExposureNotificationsClient() {
                override fun getTemporaryExposureKeyHistory(): Task<List<TemporaryExposureKey>> =
                    Tasks.forException(
                        ApiException(
                            Status(
                                CommonStatusCodes.RESOLUTION_REQUIRED,
                                "Resolution required",
                                pendingIntent
                            )
                        )
                    )
            })

        // WHEN
        val status = api.requestTemporaryExposureKeyHistory()

        // THEN
        assertTrue(status is TemporaryExposureKeysResult.RequireConsent)
        assertSame((status as TemporaryExposureKeysResult.RequireConsent).resolution, pendingIntent)
    }

    @Test
    fun `requestTemporaryExposureKeys with consent returns Success`() = runBlocking {
        // GIVEN
        val keys =
            listOf<TemporaryExposureKey>(TemporaryExposureKey.TemporaryExposureKeyBuilder().build())
        val api =
            NearbyExposureNotificationApi(object :
                FakeExposureNotificationsClient() {
                override fun getTemporaryExposureKeyHistory(): Task<List<TemporaryExposureKey>> =
                    Tasks.forResult(keys)
            })

        // WHEN
        val status = api.requestTemporaryExposureKeyHistory()

        // THEN
        assertTrue(status is TemporaryExposureKeysResult.Success)
        assertEquals((status as TemporaryExposureKeysResult.Success).keys, keys)
    }

    @Test
    fun `requestTemporaryExposureKeys with error returns UnknownError`() = runBlocking {
        // GIVEN
        val exception = ApiException(Status(CommonStatusCodes.ERROR))
        val api =
            NearbyExposureNotificationApi(object :
                FakeExposureNotificationsClient() {
                override fun getTemporaryExposureKeyHistory(): Task<List<TemporaryExposureKey>> =
                    Tasks.forException(exception)
            })

        // WHEN
        val status = api.requestTemporaryExposureKeyHistory()

        // THEN
        assertTrue(status is TemporaryExposureKeysResult.UnknownError)
        assertSame((status as TemporaryExposureKeysResult.UnknownError).exception, exception)
    }

    @Test
    fun `provideDiagnosisKeys without error removes files and returns Success`() = runBlocking {
        // GIVEN
        val file = File.createTempFile("test", "file")
        val api =
            NearbyExposureNotificationApi(object :
                FakeExposureNotificationsClient() {
                override fun provideDiagnosisKeys(
                    files: List<File>,
                    config: ExposureConfiguration,
                    token: String
                ): Task<Void> {
                    if (token != "test") {
                        throw AssertionError("Incorrect token: $token")
                    }
                    return Tasks.forResult(null)
                }
            })

        // WHEN
        val status = api.provideDiagnosisKeys(
            listOf(file),
            ExposureConfiguration.ExposureConfigurationBuilder().build(),
            "test"
        )

        // THEN
        assertTrue(status is DiagnosisKeysResult.Success)
        assertTrue(!file.exists())
    }

    @Test
    fun `provideDiagnosisKeys with generic error removes files and returns UnknownError`() =
        runBlocking {
            // GIVEN
            val file = File.createTempFile("test", "file")
            val exception = ApiException(Status.RESULT_INTERNAL_ERROR)
            val api =
                NearbyExposureNotificationApi(object :
                    FakeExposureNotificationsClient() {
                    override fun provideDiagnosisKeys(
                        files: List<File>,
                        config: ExposureConfiguration,
                        token: String
                    ): Task<Void> = Tasks.forException(exception)
                })

            try {
                // WHEN
                val status = api.provideDiagnosisKeys(
                    listOf(file),
                    ExposureConfiguration.ExposureConfigurationBuilder().build(),
                    "test"
                )

                // THEN
                assertTrue(status is DiagnosisKeysResult.UnknownError)
                assertSame((status as DiagnosisKeysResult.UnknownError).exception, exception)
                assertFalse(file.exists())
            } finally {
                file.delete()
            }
        }

    @Test
    fun `provideDiagnosisKeys with disk io removes files and returns FailedDiskIo`() =
        runBlocking {
            // GIVEN
            val file = File.createTempFile("test", "file")
            val exception = ApiException(Status(ExposureNotificationStatusCodes.FAILED_DISK_IO))
            val api =
                NearbyExposureNotificationApi(object :
                    FakeExposureNotificationsClient() {
                    override fun provideDiagnosisKeys(
                        files: List<File>,
                        config: ExposureConfiguration,
                        token: String
                    ): Task<Void> = Tasks.forException(exception)
                })

            try {
                // WHEN
                val status = api.provideDiagnosisKeys(
                    listOf(file),
                    ExposureConfiguration.ExposureConfigurationBuilder().build(),
                    "test"
                )

                // THEN
                assertTrue(status is DiagnosisKeysResult.FailedDiskIo)
                assertFalse(file.exists())
            } finally {
                file.delete()
            }
        }

    @Test
    fun `getSummary returns the ExposureSummary`() = runBlocking {
        // GIVEN
        val summary = ExposureSummary.ExposureSummaryBuilder().build()
        val api =
            NearbyExposureNotificationApi(object :
                FakeExposureNotificationsClient() {
                override fun getExposureSummary(token: String): Task<ExposureSummary> {
                    if (token != "test") {
                        throw AssertionError("Incorrect token: $token")
                    }
                    return Tasks.forResult(summary)
                }
            })

        // WHEN
        val result = api.getSummary("test")

        assertEquals(summary, result)
    }

    @Test
    fun `getSummary with error returns null`() = runBlocking {
        // GIVEN
        val api =
            NearbyExposureNotificationApi(object :
                FakeExposureNotificationsClient() {
                override fun getExposureSummary(token: String): Task<ExposureSummary> =
                    Tasks.forException(
                        ApiException(
                            Status.RESULT_INTERNAL_ERROR
                        )
                    )
            })

        // WHEN
        val result = api.getSummary("test")

        assertNull(result)
    }

    private abstract class FakeExposureNotificationsClient : ExposureNotificationClient {
        override fun isEnabled(): Task<Boolean> = Tasks.forException(IllegalStateException())

        override fun provideDiagnosisKeys(
            files: List<File>,
            config: ExposureConfiguration,
            token: String
        ): Task<Void> = Tasks.forException(IllegalStateException())

        override fun getExposureSummary(token: String): Task<ExposureSummary> =
            Tasks.forException(IllegalStateException())

        override fun start(): Task<Void> = Tasks.forException(IllegalStateException())

        override fun stop(): Task<Void> = Tasks.forException(IllegalStateException())

        override fun getExposureInformation(token: String): Task<List<ExposureInformation>> =
            Tasks.forException(IllegalStateException())

        override fun getApiKey(): ApiKey<Api.ApiOptions.NoOptions>? = null

        override fun getTemporaryExposureKeyHistory(): Task<List<TemporaryExposureKey>> =
            Tasks.forException(IllegalStateException())
    }
}
