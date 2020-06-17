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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.rijksoverheid.en.api.LabTestService
import nl.rijksoverheid.en.api.model.Registration
import nl.rijksoverheid.en.api.model.RegistrationRequest
import nl.rijksoverheid.en.enapi.TemporaryExposureKeysResult
import nl.rijksoverheid.en.enapi.nearby.ExposureNotificationApi
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException

private const val KEY_LAB_CONFIRMATION_ID = "lab_confirmation_id"
private const val KEY_BUCKET_ID = "bucket_id"
private const val KEY_CONFIRMATION_KEY = "confirmation_key"
private const val KEY_REGISTRATION_EXPIRATION = "registration_expiration"

class LabTestRepository(
    preferences: Lazy<SharedPreferences>,
    private val exposureNotificationApi: ExposureNotificationApi,
    private val api: LabTestService
) {
    private val preferences by preferences

    suspend fun requestKey(): RequestKeyResult {

        return withContext(Dispatchers.IO) {
            val code = getCachedRegistrationCode()
            if (code != null) {
                return@withContext RequestKeyResult.Success(code)
            }
            try {
                val result = api.register(RegistrationRequest())
                storeResult(result)
                RequestKeyResult.Success(result.labConfirmationId)
            } catch (ex: HttpException) {
                Timber.e(ex, "Error registering")
                RequestKeyResult.UnknownError
            } catch (ex: IOException) {
                Timber.e(ex, "Error registering")
                RequestKeyResult.UnknownError
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
                result.validitySeconds * 1000L + System.currentTimeMillis()
            )
        }
    }

    private fun getCachedRegistrationCode(): String? {
        clearExpiredKeys()
        return preferences.getString(KEY_LAB_CONFIRMATION_ID, null)
    }

    private fun clearExpiredKeys() {
        val expiration = preferences.getLong(KEY_REGISTRATION_EXPIRATION, 0)
        if (expiration > 0 && expiration < System.currentTimeMillis()) {
            preferences.edit {
                remove(KEY_CONFIRMATION_KEY)
                remove(KEY_REGISTRATION_EXPIRATION)
                remove(KEY_LAB_CONFIRMATION_ID)
                remove(KEY_BUCKET_ID)
            }
        }
    }

    suspend fun uploadDiagnosisKeys(): UploadDiagnosisKeysResult {
        return when (val result = exposureNotificationApi.requestTemporaryExposureKeyHistory()) {
            is TemporaryExposureKeysResult.RequireConsent -> UploadDiagnosisKeysResult.RequireConsent(
                result.resolution
            )
            is TemporaryExposureKeysResult.UnknownError -> UploadDiagnosisKeysResult.UnknownError
            is TemporaryExposureKeysResult.Success -> UploadDiagnosisKeysResult.Success
        }
    }

    sealed class UploadDiagnosisKeysResult {
        /**
         * Scheduled successfully or completed immediately
         */
        object Success : UploadDiagnosisKeysResult()
        object UnknownError : UploadDiagnosisKeysResult()

        /**
         * Consent is required
         */
        data class RequireConsent(val resolution: PendingIntent) : UploadDiagnosisKeysResult()
    }
}
