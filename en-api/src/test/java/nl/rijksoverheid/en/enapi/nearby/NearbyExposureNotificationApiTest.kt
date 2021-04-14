/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.enapi.nearby

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.exposurenotification.DailySummariesConfig
import com.google.android.gms.nearby.exposurenotification.DiagnosisKeysDataMapping
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes
import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import com.google.android.gms.nearby.exposurenotification.ReportType
import com.google.android.gms.nearby.exposurenotification.ScanInstance
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.en.enapi.DailyRiskScoresResult
import nl.rijksoverheid.en.enapi.DiagnosisKeysResult
import nl.rijksoverheid.en.enapi.DisableNotificationsResult
import nl.rijksoverheid.en.enapi.EnableNotificationsResult
import nl.rijksoverheid.en.enapi.StatusResult
import nl.rijksoverheid.en.enapi.TemporaryExposureKeysResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowPackageManager
import java.io.File
import java.time.LocalDate
import java.time.ZoneId


@Suppress("DEPRECATION")
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.O_MR1])
class NearbyExposureNotificationApiTest {

    private val fakeSettingsComponent =
        ComponentName("com.example", "com.example.FakeSettingsActivity")

    private lateinit var context: Context

    private val dailySummariesConfig = DailySummariesConfig.DailySummariesConfigBuilder()
        .setMinimumWindowScore(0.0)
        .setDaysSinceExposureThreshold(10)
        .setAttenuationBuckets(listOf(56, 62, 70), listOf(1.0, 1.0, 0.3, 0.0))
        .setInfectiousnessWeight(Infectiousness.STANDARD, 1.0)
        .setInfectiousnessWeight(Infectiousness.HIGH, 2.0)
        .setReportTypeWeight(ReportType.CONFIRMED_CLINICAL_DIAGNOSIS, 1.0)
        .setReportTypeWeight(ReportType.CONFIRMED_TEST, 1.0)
        .setReportTypeWeight(ReportType.SELF_REPORT, 1.0)
        .build()

    private val diagnosisKeysDataMapping = DiagnosisKeysDataMapping.DiagnosisKeysDataMappingBuilder()
        .setDaysSinceOnsetToInfectiousness((-14..14).map { it to Infectiousness.STANDARD }.toMap())
        .setReportTypeWhenMissing(ReportType.CONFIRMED_TEST)
        .setInfectiousnessWhenDaysSinceOnsetMissing(Infectiousness.STANDARD).build()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val spm: ShadowPackageManager = shadowOf(
            ApplicationProvider.getApplicationContext<Context>()
                .packageManager
        )

        spm.addActivityIfNotPresent(fakeSettingsComponent)
        spm.addIntentFilterForActivity(
            fakeSettingsComponent,
            IntentFilter(ExposureNotificationClient.ACTION_EXPOSURE_NOTIFICATION_SETTINGS)
        )
    }

    private fun removeEmulatedExposureNotificationApi() {
        val spm = shadowOf(context.packageManager)
        spm.clearIntentFilterForActivity(fakeSettingsComponent)
    }

    @Test
    fun `getStatus without available api returns Unavailable`() = runBlocking {
        removeEmulatedExposureNotificationApi()
        val api =
            NearbyExposureNotificationApi(
                context,
                object :
                    FakeExposureNotificationsClient() {
                    override fun isEnabled(): Task<Boolean> {
                        throw AssertionError()
                    }
                }
            )

        val status = api.getStatus()

        assertTrue(status is StatusResult.Unavailable)
        assertEquals(
            ExposureNotificationStatusCodes.FAILED_TEMPORARILY_DISABLED,
            (status as StatusResult.Unavailable).statusCode
        )
    }

    @Test
    fun `getStatus with enabled api returns enabled status`() = runBlocking {
        // GIVEN
        val api =
            NearbyExposureNotificationApi(
                context,
                object :
                    FakeExposureNotificationsClient() {
                    override fun isEnabled(): Task<Boolean> =
                        Tasks.forResult(true)

                }
            )

        // WHEN
        val status = api.getStatus()

        // THEN
        assertEquals(StatusResult.Enabled, status)
    }

    @Test
    fun `getStatus with disabled api returns disabled status`() = runBlocking {
        // GIVEN
        val api =
            NearbyExposureNotificationApi(
                context,
                object :
                    FakeExposureNotificationsClient() {
                    override fun isEnabled(): Task<Boolean> = Tasks.forResult(false)
                }
            )

        // WHEN
        val status = api.getStatus()

        // THEN
        assertEquals(StatusResult.Disabled, status)
    }

    @Test
    fun `getStatus with api not connected error returns unavailable status`() = runBlocking {
        // GIVEN
        val api =
            NearbyExposureNotificationApi(
                context,
                object :
                    FakeExposureNotificationsClient() {
                    override fun isEnabled(): Task<Boolean> =
                        Tasks.forException(ApiException(Status(CommonStatusCodes.API_NOT_CONNECTED)))
                }
            )

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
            NearbyExposureNotificationApi(
                context,
                object :
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
                }
            )

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
            NearbyExposureNotificationApi(
                context,
                object :
                    FakeExposureNotificationsClient() {
                    override fun isEnabled(): Task<Boolean> =
                        Tasks.forException(ApiException(Status(CommonStatusCodes.INTERNAL_ERROR)))
                }
            )

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
            NearbyExposureNotificationApi(
                context,
                object :
                    FakeExposureNotificationsClient() {
                    override fun start(): Task<Void> = Tasks.forResult(null)
                }
            )

        // WHEN
        val status = api.requestEnableNotifications()

        // THEN
        assertEquals(EnableNotificationsResult.Enabled, status)
    }

    @Test
    fun `requestEnableNotifications without API installed returns Unavailable`() = runBlocking {
        removeEmulatedExposureNotificationApi()
        val api =
            NearbyExposureNotificationApi(
                context,
                object :
                    FakeExposureNotificationsClient() {
                    override fun start(): Task<Void> = Tasks.forException(IllegalStateException())
                }
            )

        val status = api.requestEnableNotifications()

        assertTrue(status is EnableNotificationsResult.Unavailable)
        assertEquals(
            ExposureNotificationStatusCodes.FAILED_TEMPORARILY_DISABLED,
            (status as EnableNotificationsResult.Unavailable).statusCode
        )
    }

    @Test
    fun `requestEnableNotifications with errors returns UnknownError`() = runBlocking {
        // GIVEN
        val exception = RuntimeException("test")
        val api =
            NearbyExposureNotificationApi(
                context,
                object :
                    FakeExposureNotificationsClient() {
                    override fun start(): Task<Void> = Tasks.forException(exception)
                }
            )

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
                NearbyExposureNotificationApi(
                    context,
                    object :
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
                    }
                )

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
            NearbyExposureNotificationApi(
                context,
                object :
                    FakeExposureNotificationsClient() {
                    override fun stop(): Task<Void> = Tasks.forResult(null)
                }
            )

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
            NearbyExposureNotificationApi(
                context,
                object :
                    FakeExposureNotificationsClient() {
                    override fun stop(): Task<Void> = Tasks.forException(exception)
                }
            )

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
            NearbyExposureNotificationApi(
                context,
                object :
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
                }
            )

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
            NearbyExposureNotificationApi(
                context,
                object :
                    FakeExposureNotificationsClient() {
                    override fun getTemporaryExposureKeyHistory(): Task<List<TemporaryExposureKey>> =
                        Tasks.forResult(keys)
                }
            )

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
            NearbyExposureNotificationApi(
                context,
                object :
                    FakeExposureNotificationsClient() {
                    override fun getTemporaryExposureKeyHistory(): Task<List<TemporaryExposureKey>> =
                        Tasks.forException(exception)
                }
            )

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
            NearbyExposureNotificationApi(
                context,
                object :
                    FakeExposureNotificationsClient() {
                    override fun provideDiagnosisKeys(files: List<File>): Task<Void> {
                        return Tasks.forResult(null)
                    }
                    override fun setDiagnosisKeysDataMapping(p0: DiagnosisKeysDataMapping?): Task<Void> {
                        return Tasks.forResult(null)
                    }
                }
            )

        // WHEN
        val status = api.provideDiagnosisKeys(
            listOf(file),
            diagnosisKeysDataMapping,
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
                NearbyExposureNotificationApi(
                    context,
                    object :
                        FakeExposureNotificationsClient() {
                        override fun provideDiagnosisKeys(files: List<File>): Task<Void> {
                            return Tasks.forException(exception)
                        }
                        override fun setDiagnosisKeysDataMapping(p0: DiagnosisKeysDataMapping?): Task<Void> {
                            return Tasks.forResult(null)
                        }
                    }
                )

            try {
                // WHEN
                val status = api.provideDiagnosisKeys(
                    listOf(file),
                    diagnosisKeysDataMapping,
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
                NearbyExposureNotificationApi(
                    context,
                    object :
                        FakeExposureNotificationsClient() {
                        override fun provideDiagnosisKeys(files: List<File>): Task<Void> {
                            return Tasks.forException(exception)
                        }
                        override fun setDiagnosisKeysDataMapping(p0: DiagnosisKeysDataMapping?): Task<Void> {
                            return Tasks.forResult(null)
                        }
                    }
                )

            try {
                // WHEN
                val status = api.provideDiagnosisKeys(
                    listOf(file),
                    diagnosisKeysDataMapping,
                )

                // THEN
                assertTrue(status is DiagnosisKeysResult.FailedDiskIo)
                assertFalse(file.exists())
            } finally {
                file.delete()
            }
        }

    @Test
    fun `NearbyExposureNotificationApi getDailyRiskScores returns DailyRiskScores based on the exposureWindows`() = runBlocking {
        // GIVEN
        val date = LocalDate.now()
        val exposureWindows = mutableListOf(
            ExposureWindow.Builder().setDateMillisSinceEpoch(
                date.atStartOfDay(ZoneId.of("UTC"))
                    .toInstant()
                    .toEpochMilli()
            ).setInfectiousness(Infectiousness.STANDARD)
                .setScanInstances(
                    listOf(
                        ScanInstance.Builder()
                            .setSecondsSinceLastScan(600)
                            .setTypicalAttenuationDb(80).build()
                    )
                ).build()
        )
        val api = NearbyExposureNotificationApi(
            context,
            object : FakeExposureNotificationsClient() {
                override fun getExposureWindows(): Task<MutableList<ExposureWindow>> {
                    return Tasks.forResult(exposureWindows)
                }
            }
        )

        // WHEN
        val result = api.getDailyRiskScores(dailySummariesConfig)

        assertEquals(
            RiskModel(dailySummariesConfig).getDailyRiskScores(exposureWindows),
            (result as DailyRiskScoresResult.Success).dailyRiskScores
        )
    }

    @Test
    fun `getDailyRiskScores with error returns an empty map`() = runBlocking {
        // GIVEN
        val error = ApiException(Status.RESULT_INTERNAL_ERROR)
        val api =
            NearbyExposureNotificationApi(
                context,
                object :
                    FakeExposureNotificationsClient() {
                    override fun getExposureWindows(): Task<MutableList<ExposureWindow>> =
                        Tasks.forException(error)
                }
            )

        // WHEN
        val result = api.getDailyRiskScores(dailySummariesConfig)

        assertEquals(DailyRiskScoresResult.UnknownError(error), result)
    }
}
