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
import nl.rijksoverheid.en.api.RequestSize
import nl.rijksoverheid.en.api.model.AppConfig
import nl.rijksoverheid.en.api.model.PostKeysRequest
import nl.rijksoverheid.en.api.model.Registration
import nl.rijksoverheid.en.api.model.RegistrationRequest
import nl.rijksoverheid.en.config.AppConfigManager
import nl.rijksoverheid.en.enapi.TemporaryExposureKeysResult
import nl.rijksoverheid.en.enapi.nearby.ExposureNotificationApi
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlin.random.Random

private const val KEY_LAB_CONFIRMATION_ID = "lab_confirmation_id"
private const val KEY_BUCKET_ID = "bucket_id"
private const val KEY_CONFIRMATION_KEY = "confirmation_key"
private const val KEY_REGISTRATION_EXPIRATION = "registration_expiration"
private const val KEY_UPLOAD_DIAGNOSTIC_KEYS = "upload_diagnostic_keys"
private const val KEY_PENDING_KEYS = "upload_pending_keys"
private const val KEY_DID_UPLOAD = "upload_completed"

typealias UploadScheduler = () -> Unit
typealias DecoyScheduler = (Long) -> Unit

class LabTestRepository(
    preferences: Lazy<SharedPreferences>,
    private val exposureNotificationApi: ExposureNotificationApi,
    private val api: LabTestService,
    private val uploadScheduler: UploadScheduler,
    private val decoyScheduler: DecoyScheduler,
    private val appConfigManager: AppConfigManager,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val random: Random = Random
) {
    private val preferences by preferences

    suspend fun scheduleNextDecoyScheduleSequence() {
        val r = random.nextDouble()
        if (r <= appConfigManager.getCachedConfigOrDefault().decoyProbability) {
            val start = LocalDate.now(clock).atStartOfDay()
            val end = start.plusDays(1)
            val decoyTimestamp = Instant.ofEpochMilli(
                random.nextLong(
                    start.atZone(clock.zone).toInstant().toEpochMilli(),
                    end.atZone(clock.zone).toInstant().toEpochMilli()
                )
            )

            // if scheduled time has passed, schedule it or the next day
            val scheduleTime = if (decoyTimestamp.isBefore(Instant.now(clock))) {
                decoyTimestamp.plus(1, ChronoUnit.DAYS)
            } else {
                decoyTimestamp
            }

            val delay = Duration.between(
                ZonedDateTime.now(clock).toInstant(),
                scheduleTime
            ).toMillis().coerceAtLeast(TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES))
            Timber.d("Schedule decoy at $scheduleTime, with delay $delay")
            decoyScheduler(delay)
        } else {
            Timber.d("Decoy is skipped")
        }
    }

    suspend fun registerForUpload(): RegistrationResult {
        return withContext(Dispatchers.IO) {
            val code = getCachedRegistrationCode()
            if (code != null) {
                return@withContext RegistrationResult.Success(code)
            }
            try {
                val config = appConfigManager.getCachedConfigOrDefault()
                val result = api.register(RegistrationRequest(), config.requestSize)
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
        if (expiration == 0L || expiration < clock.millis()) {
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
            remove(KEY_DID_UPLOAD)
        }
    }

    suspend fun uploadDiagnosticKeysIfPending(): UploadDiagnosticKeysResult {
        val pending = preferences.getBoolean(KEY_UPLOAD_DIAGNOSTIC_KEYS, false) &&
            !preferences.getBoolean(KEY_DID_UPLOAD, false)
        clearKeyDataIfExpired()
        return if (preferences.getBoolean(KEY_UPLOAD_DIAGNOSTIC_KEYS, false)) {
            uploadDiagnosticKeys()
        } else {
            if (pending) {
                UploadDiagnosticKeysResult.Expired
            } else {
                UploadDiagnosticKeysResult.Success
            }
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
            uploadKeys(exposureKeys, bucketId, confirmationKey)
            preferences.edit {
                putBoolean(KEY_DID_UPLOAD, true)
            }
            keyStorage.clearKeys()
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
        val config = appConfigManager.getCachedConfigOrDefault()
        val request = PostKeysRequest(requestedKeys.map {
            nl.rijksoverheid.en.api.model.TemporaryExposureKey(
                it.keyData,
                it.rollingStartIntervalNumber,
                it.rollingPeriod
            )
        }, bucketId)
        api.postKeys(request, HmacSecret(confirmationKey), config.requestSize)
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

    suspend fun sendDecoyTraffic(): SendDecoyResult {
        if (getCachedRegistrationCode() == null) {
            registerForUpload()
            return SendDecoyResult.Registered(getDecoyRegistrationDelay())
        }

        val key = ByteArray(16)
        SecureRandom().nextBytes(key)

        val bucketId = preferences.getString(KEY_BUCKET_ID, null) ?: throw IllegalStateException()
        val fakeSecret = ByteArray(16)
        SecureRandom().nextBytes(fakeSecret)

        val request = PostKeysRequest(
            listOf(
                nl.rijksoverheid.en.api.model.TemporaryExposureKey(
                    key,
                    (clock.millis() / 600000L).toInt(), 144
                )
            ), generateDecoyBucketId(bucketId.length)
        )

        api.stopKeys(
            request,
            HmacSecret(fakeSecret),
            appConfigManager.getCachedConfigOrDefault().requestSize
        )

        return SendDecoyResult.Success
    }

    private fun getDecoyRegistrationDelay() = if (random.nextInt(0, 10) == 0) {
        random.nextLong(1000, 20 * 60 * 60 * 1000L)
    } else {
        random.nextLong(1000, 900 * 1000L)
    }

    private fun generateDecoyBucketId(size: Int): String {
        val source = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890"
        return (1..size)
            .map { source.random() }
            .joinToString("")
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
         * The key upload could not be completed before the registration expired
         */
        object Expired : UploadDiagnosticKeysResult()

        /**
         * An error occurred and the key upload should be retried later
         */
        object Retry : UploadDiagnosticKeysResult()
    }

    sealed class SendDecoyResult {
        object Success : SendDecoyResult()
        data class Registered(val delayMillis: Long) : SendDecoyResult()
    }
}

private val AppConfig.requestSize
    get() = RequestSize(requestMinimumSize, requestMaximumSize)
