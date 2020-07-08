/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest

import android.app.PendingIntent
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.rijksoverheid.en.api.HmacSecret
import nl.rijksoverheid.en.api.LabTestService
import nl.rijksoverheid.en.api.model.PostKeysRequest
import nl.rijksoverheid.en.api.model.Registration
import nl.rijksoverheid.en.api.model.RegistrationRequest
import nl.rijksoverheid.en.enapi.TemporaryExposureKeysResult
import nl.rijksoverheid.en.enapi.nearby.ExposureNotificationApi
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.time.Clock

private const val KEY_LAB_CONFIRMATION_ID = "lab_confirmation_id"
private const val KEY_BUCKET_ID = "bucket_id"
private const val KEY_CONFIRMATION_KEY = "confirmation_key"
private const val KEY_REGISTRATION_EXPIRATION = "registration_expiration"
private const val KEY_UPLOAD_DIAGNOSTIC_KEYS = "upload_diagnostic_keys"
private const val KEY_PENDING_KEYS = "upload_pending_keys"

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
            putString(
                KEY_CONFIRMATION_KEY,
                Base64.encodeToString(result.confirmationKey, Base64.NO_WRAP)
            )
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
            remove(KEY_PENDING_KEYS)
            remove(KEY_UPLOAD_DIAGNOSTIC_KEYS)
        }
    }

    suspend fun uploadDiagnosticKeysIfPending(): UploadDiagnosticKeysResult {
        clearKeyDataIfExpired()
        return if (preferences.getBoolean(KEY_UPLOAD_DIAGNOSTIC_KEYS, false)) {
            uploadDiagnosticKeys()
        } else {
            UploadDiagnosticKeysResult.Success
        }
    }

    /**
     * Upload the diagnostic keys
     */
    private suspend fun uploadDiagnosticKeys(): UploadDiagnosticKeysResult {
        val confirmationKey =
            preferences.getString(KEY_CONFIRMATION_KEY, null)
                ?.let { Base64.decode(it, Base64.NO_WRAP) } ?: throw IllegalStateException()
        val bucketId = preferences.getString(KEY_BUCKET_ID, null) ?: throw IllegalStateException()
        val keyStorage = KeysStorage(KEY_PENDING_KEYS, preferences)
        val exposureKeys = keyStorage.getKeys()
        return try {
            if (exposureKeys.isNotEmpty()) {
                uploadKeys(exposureKeys, bucketId, confirmationKey)
            }
            clearKeyData()
            UploadDiagnosticKeysResult.Success
        } catch (ex: HttpException) {
            Timber.e(ex, "Error while uploading keys")
            UploadDiagnosticKeysResult.Retry
        } catch (ex: IOException) {
            Timber.e(ex, "Error while uploading keys")
            UploadDiagnosticKeysResult.Retry
        }
    }

    private suspend fun uploadKeys(
        requestedKeys: List<TemporaryExposureKey>,
        bucketId: String,
        confirmationKey: ByteArray
    ) {
        val request = PostKeysRequest(requestedKeys.map {
            nl.rijksoverheid.en.api.model.TemporaryExposureKey(
                it.keyData,
                it.rollingStartIntervalNumber,
                it.rollingPeriod
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
                val keyStorage = KeysStorage(KEY_PENDING_KEYS, preferences)
                keyStorage.storeKeys(result.keys)
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
         * The key upload has completed
         */
        object Success : UploadDiagnosticKeysResult()

        /**
         * An error occurred and the key upload should be retried later
         */
        object Retry : UploadDiagnosticKeysResult()
    }
}

// TODO pad the request so that it looks like a full key upload
private fun PostKeysRequest.adjustPadding(): PostKeysRequest = this
