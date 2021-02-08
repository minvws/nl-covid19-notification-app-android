/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
package nl.rijksoverheid.en.enapi.nearby

import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.nearby.exposurenotification.DailySummariesConfig
import com.google.android.gms.nearby.exposurenotification.DailySummary
import com.google.android.gms.nearby.exposurenotification.DiagnosisKeysDataMapping
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import com.google.android.gms.nearby.exposurenotification.ReportType
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.rijksoverheid.en.enapi.DiagnosisKeysResult
import nl.rijksoverheid.en.enapi.DisableNotificationsResult
import nl.rijksoverheid.en.enapi.EnableNotificationsResult
import nl.rijksoverheid.en.enapi.ExposureNotificationApi
import nl.rijksoverheid.en.enapi.StatusResult
import nl.rijksoverheid.en.enapi.TemporaryExposureKeysResult
import okio.Okio
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Wrapper around [ExposureNotificationClient] implementing [ExposureNotificationApi]
 */
class NearbyExposureNotificationApi(
    private val context: Context,
    private val client: ExposureNotificationClient
) :
    ExposureNotificationApi {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    /**
     * Get the status of the exposure notifications api
     * @return the status
     */
    override suspend fun getStatus(): StatusResult = suspendCoroutine { c ->
        if (isApiAvailable()) {
            client.isEnabled.addOnSuccessListener {
                if (it) {
                    c.resume(StatusResult.Enabled)
                } else {
                    c.resume(StatusResult.Disabled)
                }
            }.addOnFailureListener {
                Timber.e(it, "Error getting API status")
                val apiException = it as? ApiException
                c.resume(
                    when (apiException?.statusCode) {
                        ExposureNotificationStatusCodes.API_NOT_CONNECTED -> StatusResult.Unavailable(
                            apiException.getMostSpecificStatusCode()
                        )
                        else -> StatusResult.UnknownError(
                            it
                        )
                    }
                )
            }
        } else {
            c.resume(
                StatusResult.Unavailable(
                    ExposureNotificationStatusCodes.FAILED_TEMPORARILY_DISABLED
                )
            )
        }
    }

    /**
     * Request to enable Exposure Notifications
     * @return the result of the request
     */
    override suspend fun requestEnableNotifications(): EnableNotificationsResult =
        suspendCoroutine { c ->
            if (isApiAvailable()) {
                client.start().addOnSuccessListener {
                    c.resume(EnableNotificationsResult.Enabled)
                }.addOnFailureListener {
                    val apiException = it as? ApiException
                    val status = apiException?.getMostSpecificStatusCode()
                    c.resume(
                        when (status) {
                            CommonStatusCodes.RESOLUTION_REQUIRED -> EnableNotificationsResult.ResolutionRequired(
                                apiException.status.resolution!!
                            )
                            null -> {
                                Timber.e(it, "Error while enabling notifications")
                                EnableNotificationsResult.UnknownError(
                                    it
                                )
                            }
                            else -> {
                                Timber.e(it, "Error while enabling notifications, status = $status")
                                EnableNotificationsResult.Unavailable(
                                    status
                                )
                            }
                        }
                    )
                }
            } else {
                c.resume(
                    EnableNotificationsResult.Unavailable(
                        ExposureNotificationStatusCodes.FAILED_TEMPORARILY_DISABLED
                    )
                )
            }
        }

    /**
     * Request to disable exposure notifications
     * @return the result
     */
    override suspend fun disableNotifications(): DisableNotificationsResult =
        suspendCoroutine { c ->
            client.stop().addOnSuccessListener {
                c.resume(DisableNotificationsResult.Disabled)
            }.addOnFailureListener {
                Timber.e(it, "Error while disabling notifications")
                c.resume(
                    // Technically we could get a connection error, but this is not
                    // really expected, since this is only called when previously enabled.
                    DisableNotificationsResult.UnknownError(
                        it
                    )
                )
            }
        }

    /**
     * Request the temporary exposure key history
     * @return the result. For the initial call is is most likely [TemporaryExposureKeysResult.RequireConsent]
     */
    override suspend fun requestTemporaryExposureKeyHistory(): TemporaryExposureKeysResult =
        suspendCoroutine { c ->
            client.temporaryExposureKeyHistory.addOnSuccessListener {
                c.resume(
                    TemporaryExposureKeysResult.Success(
                        it
                    )
                )
            }.addOnFailureListener {
                val apiException = it as? ApiException
                c.resume(
                    when (apiException?.statusCode) {
                        ExposureNotificationStatusCodes.RESOLUTION_REQUIRED -> TemporaryExposureKeysResult.RequireConsent(
                            apiException.status.resolution!!
                        )
                        else -> TemporaryExposureKeysResult.UnknownError(
                            it
                        )
                    }
                )
            }
        }

    /**
     * Provide the diagnostics keys for exposure notifications matching
     *
     * @param files the list of files to process. The files will be deleted after processing
     * @param configuration the configuration to use for matching
     * @return the result
     */
    override suspend fun provideDiagnosisKeys(
        files: List<File>,
        configuration: ExposureConfiguration,
    ): DiagnosisKeysResult {
        persistExposureConfiguration(configuration)
        return suspendCoroutine { c ->
            val daysToInfectiousness = mutableMapOf<Int, Int>()
            for (i in -14..14) {
                when (i) {
                    in -5..-3 -> daysToInfectiousness[i] = Infectiousness.STANDARD
                    in -2..5 -> daysToInfectiousness[i] = Infectiousness.HIGH
                    in 6..10 -> daysToInfectiousness[i] = Infectiousness.STANDARD
                    else -> daysToInfectiousness[i] = Infectiousness.NONE
                }
            }
            client.setDiagnosisKeysDataMapping(
                DiagnosisKeysDataMapping.DiagnosisKeysDataMappingBuilder()
                    //TODO set this to the correct mapping if days_since_onset is supplied
                    .setDaysSinceOnsetToInfectiousness(daysToInfectiousness)
                    .setReportTypeWhenMissing(ReportType.CONFIRMED_TEST)
                    .setInfectiousnessWhenDaysSinceOnsetMissing(Infectiousness.STANDARD).build()
            ).continueWith {
                client.provideDiagnosisKeys(files).apply {
                    addOnSuccessListener {
                        c.resume(DiagnosisKeysResult.Success)
                    }.addOnFailureListener {
                        Timber.e(it, "Error while providing diagnosis keys")
                        val apiException = it as? ApiException
                        c.resume(
                            when (apiException?.statusCode) {
                                ExposureNotificationStatusCodes.FAILED_DISK_IO -> DiagnosisKeysResult.FailedDiskIo
                                else -> DiagnosisKeysResult.UnknownError(
                                    it
                                )
                            }
                        )
                    }.addOnCompleteListener {
                        files.forEach { it.delete() }
                    }
                }
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun persistExposureConfiguration(config: ExposureConfiguration) =
        withContext(Dispatchers.IO) {
            val persistedConfig = PersistedConfig(
                config.daysSinceLastExposureScores.toList(),
                config.transmissionRiskScores.toList(),
                config.minimumRiskScore,
                config.durationScores.toList(),
                config.attenuationScores.toList()
            )
            FileOutputStream(File(context.filesDir, "exposure_config")).use {
                val sink = Okio.buffer(Okio.sink(it))
                moshi.adapter(PersistedConfig::class.java).toJson(sink, persistedConfig)
                sink.flush()
            }
        }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun loadExposureConfiguration() = withContext(Dispatchers.IO) {
        FileInputStream(File(context.filesDir, "exposure_config")).use {
            val persisted =
                moshi.adapter(PersistedConfig::class.java).fromJson(Okio.buffer(Okio.source(it)))
                    ?: throw IllegalStateException("config not persisted")
            ExposureConfiguration.ExposureConfigurationBuilder()
                .setDurationScores(*persisted.durationScores.toIntArray())
                .setTransmissionRiskScores(*persisted.transmissionRiskScores.toIntArray())
                .setMinimumRiskScore(persisted.minimumRiskScore)
                .setDaysSinceLastExposureScores(*persisted.daysSinceLastExposureScores.toIntArray())
                .setAttenuationScores(*persisted.attenuationScores.toIntArray()).build()
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun loadDailySummariesConfig() = withContext(Dispatchers.IO) {
        FileInputStream(File(context.filesDir, "exposure_config")).use {
            //val persisted =
            //    moshi.adapter(PersistedConfig::class.java).fromJson(Okio.buffer(Okio.source(it)))
            //        ?: throw IllegalStateException("config not persisted")
            //TODO replace with our risk params
            DailySummariesConfig.DailySummariesConfigBuilder()
                // Don't filter based on ExposureWindow scores.
                .setMinimumWindowScore(0.0)
                // Include exposures for only the last 10 days.
                .setDaysSinceExposureThreshold(10)
                // Upweight attenuations indicating very close exposures.
                // Downweight attenuations where distance is less certain.
                .setAttenuationBuckets(listOf(56, 62, 70), listOf(1.0, 1.0, 0.3, 0.0))
                // Double High Infectiousness weight and drop when none
                .setInfectiousnessWeight(Infectiousness.STANDARD, 1.0)
                .setInfectiousnessWeight(Infectiousness.HIGH, 2.0)
                // Include all report types.
                .setReportTypeWeight(ReportType.CONFIRMED_CLINICAL_DIAGNOSIS, 1.0)
                .setReportTypeWeight(ReportType.CONFIRMED_TEST, 1.0)
                .setReportTypeWeight(ReportType.SELF_REPORT, 1.0)
                .build()
        }
    }

    /**
     * Get the [ExposureSummary] by token
     * @param token the token passed to [provideDiagnosisKeys] and from [ExposureNotificationClient.EXTRA_TOKEN]
     * @return the summary or null if there's no match or an error occurred
     */
    @Deprecated("Should be replaced by getDailySummaries")
    suspend fun getSummary(token: String): ExposureSummary? {
        return if (token == ExposureNotificationClient.TOKEN_A) {
            val windows = getExposureWindows()
            LegacyRiskModel(
                loadExposureConfiguration()
            ).getSummary(windows)
        } else {
            suspendCoroutine { c ->
                client.getExposureSummary(token).addOnSuccessListener {
                    c.resume(it)
                }.addOnFailureListener {
                    Timber.e(it, "Error getting ExposureSummary")
                    // TODO determine if we want bubble up errors here; this is used
                    // when processing the notification and at that point the API should never return
                    // null. If it does or throws an error, all we can do is retry or give up
                    c.resume(null)
                }
            }
        }
    }

    /**
     * Get the [ExposureSummary] by token
     * @return a list of DailySummary objects corresponding to the last 14 days of exposure data or null if there's no match or an error occurred
     */
    override suspend fun getDailySummaries(): List<DailySummary>? {
        val config = loadDailySummariesConfig()
        return suspendCoroutine { c ->
            client.getDailySummaries(config).addOnSuccessListener {
                c.resume(it)
            }.addOnFailureListener {
                Timber.e(it, "Error getting DailySummaries")
                // TODO determine if we want bubble up errors here; this is used
                // when processing the notification and at that point the API should never return
                // null. If it does or throws an error, all we can do is retry or give up
                c.resume(null)
            }
        }
    }

    override suspend fun getDailyRiskScores(scoreType: RiskModel.ScoreType): Map<Long, Double> {
        return RiskModel(loadDailySummariesConfig())
            .getDailyRiskScores(getExposureWindows(), scoreType)
    }

    private suspend fun getExposureWindows(): List<ExposureWindow> = suspendCoroutine { c ->
        client.exposureWindows.addOnSuccessListener {
            c.resume(it)
        }.addOnFailureListener {
            c.resume(emptyList())
        }
    }

    private fun isApiAvailable(): Boolean {
        return context.packageManager.resolveActivity(
            Intent(ExposureNotificationClient.ACTION_EXPOSURE_NOTIFICATION_SETTINGS),
            0
        ) != null
    }

    override fun deviceSupportsLocationlessScanning(): Boolean {
        return client.deviceSupportsLocationlessScanning()
    }
}

/**
 * Try to get the (more specific) status code if this [ApiException] has status [CommonStatusCodes.API_NOT_CONNECTED]
 * @return the status code that was parsed in case of [CommonStatusCodes.API_NOT_CONNECTED] or the original status code otherwise
 */
private fun ApiException.getMostSpecificStatusCode(): Int {
    val statusMessage = status.statusMessage
    if (statusCode == ExposureNotificationStatusCodes.API_NOT_CONNECTED && statusMessage != null) {
        val matches = Regex("ConnectionResult\\{statusCode=[a-zA-Z0-9_]+\\(([0-9]+)\\),").find(
            statusMessage
        )
        if (matches != null && matches.groupValues.size == 2) {
            return matches.groupValues[1].toIntOrNull() ?: statusCode
        }
    }
    return statusCode
}

@JsonClass(generateAdapter = true)
@Keep
internal class PersistedConfig(
    val daysSinceLastExposureScores: List<Int>,
    val transmissionRiskScores: List<Int>,
    val minimumRiskScore: Int,
    val durationScores: List<Int>,
    val attenuationScores: List<Int>
)