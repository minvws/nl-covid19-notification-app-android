/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest

import android.app.PendingIntent
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.rijksoverheid.en.api.HmacSecret
import nl.rijksoverheid.en.api.LabTestService
import nl.rijksoverheid.en.api.model.PostKeysRequest
import nl.rijksoverheid.en.api.model.Registration
import nl.rijksoverheid.en.api.model.RegistrationRequest
import nl.rijksoverheid.en.api.model.TemporaryExposureKey.Companion.DEFAULT_ROLLING_PERIOD
import nl.rijksoverheid.en.enapi.TemporaryExposureKeysResult
import nl.rijksoverheid.en.enapi.nearby.ExposureNotificationApi
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime

private const val KEY_LAB_CONFIRMATION_ID = "lab_confirmation_id"
private const val KEY_BUCKET_ID = "bucket_id"
private const val KEY_CONFIRMATION_KEY = "confirmation_key"
private const val KEY_REGISTRATION_EXPIRATION = "registration_expiration"
private const val KEY_UPLOAD_DIAGNOSTIC_KEYS = "upload_diagnostic_keys"
private const val KEY_DIAGNOSTIC_KEY_START_INTERVAL = "upload_diagnostic_key_start_interval"

typealias UploadScheduler = () -> Unit

class LabTestRepository(
    preferences: Lazy<SharedPreferences>,
    private val exposureNotificationApi: ExposureNotificationApi,
    private val api: LabTestService,
    private val uploadScheduler: UploadScheduler,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    private val preferences by preferences

    suspend fun registerForUpload(): RegistrationResult {
        return withContext(Dispatchers.IO) {
            val code = getCachedRegistrationCode()
            if (code != null) {
                return@withContext RegistrationResult.Success(code)
            }
            try {
                val result = api.register(RegistrationRequest())
                storeResult(result)
                RegistrationResult.Success(result.labConfirmationId)
            } catch (ex: HttpException) {
                Timber.e(ex, "Error registering")
                RegistrationResult.UnknownError
            } catch (ex: IOException) {
                Timber.e(ex, "Error registering")
                RegistrationResult.UnknownError
            }
        }
    }

    private fun storeResult(result: Registration) {
        preferences.edit {
            putString(KEY_CONFIRMATION_KEY, result.confirmationKey)
            putString(KEY_BUCKET_ID, result.bucketId)
            putString(KEY_LAB_CONFIRMATION_ID, result.labConfirmationId)
            putLong(
                KEY_REGISTRATION_EXPIRATION,
                (result.validitySeconds * 1000L) + clock.millis()
            )
        }
    }

    private fun getCachedRegistrationCode(): String? {
        clearKeyDataIfExpired()
        return preferences.getString(KEY_LAB_CONFIRMATION_ID, null)
    }

    private fun clearKeyDataIfExpired() {
        val expiration = preferences.getLong(KEY_REGISTRATION_EXPIRATION, 0)
        if (expiration > 0 && expiration < clock.millis()) {
            clearKeyData()
        }
    }

    private fun clearKeyData() {
        preferences.edit {
            remove(KEY_CONFIRMATION_KEY)
            remove(KEY_REGISTRATION_EXPIRATION)
            remove(KEY_LAB_CONFIRMATION_ID)
            remove(KEY_BUCKET_ID)
            remove(KEY_DIAGNOSTIC_KEY_START_INTERVAL)
            remove(KEY_UPLOAD_DIAGNOSTIC_KEYS)
        }
    }

    suspend fun uploadDiagnosticKeysOrDecoy(): UploadDiagnosticKeysResult {
        clearKeyDataIfExpired()
        return if (preferences.getBoolean(KEY_UPLOAD_DIAGNOSTIC_KEYS, false)) {
            uploadDiagnosticKeys()
        } else {
            uploadDecoyKeys()
        }
    }

    private fun uploadDecoyKeys(): UploadDiagnosticKeysResult = UploadDiagnosticKeysResult.Completed

    /**
     * Upload the diagnostic keys
     */
    private suspend fun uploadDiagnosticKeys(): UploadDiagnosticKeysResult {
        val confirmationKey =
            preferences.getString(KEY_CONFIRMATION_KEY, null) ?: throw IllegalStateException()
        val bucketId = preferences.getString(KEY_BUCKET_ID, null) ?: throw IllegalStateException()
        return when (val keyResult = exposureNotificationApi.requestTemporaryExposureKeyHistory()) {
            is TemporaryExposureKeysResult.RequireConsent -> {
                Timber.d("Consent has expired")
                clearKeyData()
                UploadDiagnosticKeysResult.Completed
            }
            is TemporaryExposureKeysResult.UnknownError -> {
                Timber.w(
                    keyResult.exception,
                    "Error getting keys from the exposure notification api"
                )
                UploadDiagnosticKeysResult.Retry
            }
            is TemporaryExposureKeysResult.Success -> {
                val lastKeyStartInterval = preferences.getLong(
                    KEY_DIAGNOSTIC_KEY_START_INTERVAL, 0L
                )
                val requestedKeys =
                    keyResult.keys.filter { it.rollingStartIntervalNumber > lastKeyStartInterval }
                if (requestedKeys.isNotEmpty()) {
                    try {
                        uploadKeys(requestedKeys, bucketId, confirmationKey)
                        if (lastKeyStartInterval == 0L) {
                            Timber.d("First key upload")
                            // this was the first upload, mark the latest key time.
                            val latestKey = requestedKeys.maxBy { it.rollingStartIntervalNumber }!!
                            preferences.edit {
                                putLong(
                                    KEY_DIAGNOSTIC_KEY_START_INTERVAL,
                                    latestKey.rollingStartIntervalNumber.toLong()
                                )
                            }
                            UploadDiagnosticKeysResult.Initial(
                                LocalDateTime.ofInstant(
                                    Instant.ofEpochMilli(
                                        (latestKey.rollingStartIntervalNumber + latestKey.rollingPeriodOrDefault) * 600 * 1000L
                                    ), clock.zone
                                )
                            )
                        } else {
                            // this was the final upload and we're done now
                            clearKeyData()
                            UploadDiagnosticKeysResult.Completed
                        }
                    } catch (ex: HttpException) {
                        Timber.e(ex, "Error while uploading keys")
                        UploadDiagnosticKeysResult.Retry
                    } catch (ex: IOException) {
                        Timber.e(ex, "Error while uploading keys")
                        UploadDiagnosticKeysResult.Retry
                    }
                } else {
                    if (lastKeyStartInterval == 0L) {
                        Timber.w("Did not upload keys, but no keys returned from the API!")
                        UploadDiagnosticKeysResult.Retry
                    } else {
                        Timber.w("Did not return new keys")
                        UploadDiagnosticKeysResult.Initial(
                            LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(
                                    (lastKeyStartInterval + DEFAULT_ROLLING_PERIOD) * 600 * 1000L
                                ), clock.zone
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun uploadKeys(
        requestedKeys: List<TemporaryExposureKey>,
        bucketId: String,
        confirmationKey: String
    ) {
        val request = PostKeysRequest(requestedKeys.map {
            nl.rijksoverheid.en.api.model.TemporaryExposureKey(
                it.keyData,
                it.rollingStartIntervalNumber,
                it.rollingPeriodOrDefault
            )
        }, bucketId).adjustPadding()
        api.postKeys(request, HmacSecret(confirmationKey))
    }

    suspend fun requestUploadDiagnosticKeys(): RequestUploadDiagnosisKeysResult {
        return when (val result = exposureNotificationApi.requestTemporaryExposureKeyHistory()) {
            is TemporaryExposureKeysResult.RequireConsent -> RequestUploadDiagnosisKeysResult.RequireConsent(
                result.resolution
            )
            is TemporaryExposureKeysResult.UnknownError -> RequestUploadDiagnosisKeysResult.UnknownError
            is TemporaryExposureKeysResult.Success -> {
                if (!(preferences.contains(KEY_CONFIRMATION_KEY) && preferences.contains(
                        KEY_BUCKET_ID
                    ))
                ) {
                    throw IllegalStateException()
                }
                preferences.edit {
                    putBoolean(KEY_UPLOAD_DIAGNOSTIC_KEYS, true)
                }
                uploadScheduler()
                RequestUploadDiagnosisKeysResult.Success
            }
        }
    }

    sealed class RequestUploadDiagnosisKeysResult {
        /**
         * Scheduled successfully or completed immediately
         */
        object Success : RequestUploadDiagnosisKeysResult()
        object UnknownError : RequestUploadDiagnosisKeysResult()

        /**
         * Consent is required
         */
        data class RequireConsent(val resolution: PendingIntent) :
            RequestUploadDiagnosisKeysResult()
    }

    sealed class UploadDiagnosticKeysResult {
        /**
         * The initial set of keys has been successfully uploaded.
         */
        data class Initial(
            /**
             * Date time when the next key will be available
             **/
            val dateTime: LocalDateTime
        ) : UploadDiagnosticKeysResult()

        /**
         * The key upload has completed
         */
        object Completed : UploadDiagnosticKeysResult()

        /**
         * An error occurred and the key upload should be retried later
         */
        object Retry : UploadDiagnosticKeysResult()
    }
}

// TODO pad the request so that it looks like a full key upload
private fun PostKeysRequest.adjustPadding(): PostKeysRequest = this

private val TemporaryExposureKey.rollingPeriodOrDefault
    get() = if (rollingPeriod == 0) DEFAULT_ROLLING_PERIOD else rollingPeriod
