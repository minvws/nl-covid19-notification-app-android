/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.SharedPreferences
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Base64
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.core.location.LocationManagerCompat
import androidx.navigation.NavDeepLinkBuilder
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import nl.rijksoverheid.en.api.CdnService
import nl.rijksoverheid.en.api.model.AppConfig
import nl.rijksoverheid.en.api.model.Manifest
import nl.rijksoverheid.en.enapi.DiagnosisKeysResult
import nl.rijksoverheid.en.enapi.DisableNotificationsResult
import nl.rijksoverheid.en.enapi.EnableNotificationsResult
import nl.rijksoverheid.en.enapi.StatusResult
import nl.rijksoverheid.en.enapi.nearby.ExposureNotificationApi
import nl.rijksoverheid.en.job.ProcessManifestWorkerScheduler
import nl.rijksoverheid.en.signing.ResponseSignatureValidator
import nl.rijksoverheid.en.signing.SignatureValidationException
import nl.rijksoverheid.en.util.formatDaysSince
import okhttp3.ResponseBody
import okio.ByteString.Companion.toByteString
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val KEY_LAST_TOKEN_ID = "last_token_id"
private const val KEY_LAST_TOKEN_EXPOSURE_DATE = "last_token_exposure_date"
private const val KEY_EXPOSURE_KEY_SETS = "exposure_key_sets"
private const val KEY_LAST_KEYS_PROCESSED = "last_keys_processed"
private const val DEFAULT_MANIFEST_INTERVAL_MINUTES = 240
private const val DEBUG_TOKEN = "TEST-TOKEN"
private const val KEY_PROCESSING_OVERDUE_THRESHOLD_MINUTES = 24 * 60
private const val ID_EXPOSURE_PUSH_NOTIFICATION = 0

class ExposureNotificationsRepository(
    private val context: Context,
    private val exposureNotificationsApi: ExposureNotificationApi,
    private val api: CdnService,
    private val preferences: SharedPreferences,
    private val manifestWorkerScheduler: ProcessManifestWorkerScheduler,
    private val appLifecycleManager: AppLifecycleManager,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val signatureValidation: Boolean = BuildConfig.EXPOSURE_FILE_SIGNATURE_CHECK
) {

    val keyProcessingOverdue: Boolean
        get() {
            val timestamp = preferences.getLong(KEY_LAST_KEYS_PROCESSED, 0)
            return if (timestamp > 0) {
                Duration.between(Instant.ofEpochMilli(timestamp), clock.instant())
                    .toMinutes() > KEY_PROCESSING_OVERDUE_THRESHOLD_MINUTES
            } else {
                false
            }
        }

    suspend fun requestEnableNotifications(): EnableNotificationsResult {
        val result = exposureNotificationsApi.requestEnableNotifications()
        if (result == EnableNotificationsResult.Enabled) {
            preferences.edit {
                // reset the timer
                putLong(KEY_LAST_KEYS_PROCESSED, System.currentTimeMillis())
            }
            val interval = getLatestAppConfigOrNull()?.updatePeriodMinutes
                ?: DEFAULT_MANIFEST_INTERVAL_MINUTES
            manifestWorkerScheduler.schedule(interval)
        }
        return result
    }

    suspend fun requestDisableNotifications(): DisableNotificationsResult {
        manifestWorkerScheduler.cancel()
        preferences.edit {
            remove(KEY_LAST_KEYS_PROCESSED)
        }
        return exposureNotificationsApi.disableNotifications()
    }

    suspend fun getStatus(): StatusResult {
        val result = exposureNotificationsApi.getStatus()
        return if (result == StatusResult.Enabled) {
            if (isBluetoothEnabled() && isLocationEnabled()) {
                StatusResult.Enabled
            } else {
                StatusResult.InvalidPreconditions
            }
        } else {
            result
        }
    }

    private fun isBluetoothEnabled(): Boolean {
        val manager = context.getSystemService(BluetoothManager::class.java) ?: return false
        return manager.adapter.isEnabled
    }

    private fun isLocationEnabled(): Boolean {
        return context.getSystemService(LocationManager::class.java)
            ?.let { LocationManagerCompat.isLocationEnabled(it) } ?: true
    }

    /**
     * Downloads new exposure key sets from the server and processes them
     */
    @VisibleForTesting
    internal suspend fun processExposureKeySets(manifest: Manifest): ProcessExposureKeysResult =
        coroutineScope {
            val processedSets = preferences.getStringSet(KEY_EXPOSURE_KEY_SETS, emptySet())!!

            val updates = manifest.exposureKeysSetIds.toMutableSet().apply {
                removeAll(processedSets)
            }
            val files = updates.map {
                it to
                    async { api.getExposureKeySetFile(it) }
            }.map { (id, responseAsync) ->
                writeExposureKeyFile(id, responseAsync)
            }

            // files that have downloaded and saved successfully
            val validFiles = files.filter { it.file != null && it.signatureValid }
            val signatureErrors = files.filter { it.file != null && !it.signatureValid }
            val hasErrors = validFiles.size != files.size - signatureErrors.size

            if (signatureErrors.isNotEmpty()) {
                // mark as processed, but skip
                Timber.e("Signature validation error for ${signatureErrors.size} files")
            }

            if (validFiles.isEmpty()) {
                // shortcut if nothing to process
                return@coroutineScope if (!hasErrors) {
                    // mark any signature failures as completed
                    updateProcessedExposureKeySets(signatureErrors.map { it.id }.toSet(), manifest)
                    ProcessExposureKeysResult.Success
                } else {
                    ProcessExposureKeysResult.ServerError
                }
            }

            val configuration = try {
                getConfigurationFromManifest(manifest)
            } catch (ex: IOException) {
                Timber.e(ex, "Error fetching configuration")
                return@coroutineScope ProcessExposureKeysResult.Error(ex)
            }

            Timber.d("Processing ${validFiles.size} files")

            val result = exposureNotificationsApi.provideDiagnosisKeys(
                validFiles.map { it.file!! },
                configuration,
                createToken()
            )
            when (result) {
                is DiagnosisKeysResult.Success -> {
                    // mark all keys processed
                    Timber.d("Processing was successful")
                    updateProcessedExposureKeySets(
                        validFiles.map { it.id }.toSet() + signatureErrors.map { it.id }.toSet(),
                        manifest
                    )
                    if (!hasErrors) {
                        ProcessExposureKeysResult.Success
                    } else {
                        // postponed server error due to downloading files
                        ProcessExposureKeysResult.ServerError
                    }
                }
                else -> {
                    Timber.e("Returned an error: $result")
                    ProcessExposureKeysResult.ExposureApiError(result)
                }
            }
        }

    /**
     * Store the ids of the processed exposure keys, considering the current keys and the manifest
     * @param processed the keys that have been successfully processed from the given manifest
     * @param manifest the manifest
     */
    @WorkerThread
    private fun updateProcessedExposureKeySets(processed: Set<String>, manifest: Manifest) {
        // the ids we have previously processed + the newly processed ids
        val currentProcessedIds =
            preferences.getStringSet(KEY_EXPOSURE_KEY_SETS, emptySet())!!.toMutableSet().apply {
                addAll(processed)
            }
        // store the set of ids that are in the manifest and are processed
        preferences.edit(commit = true) {
            putStringSet(
                KEY_EXPOSURE_KEY_SETS,
                currentProcessedIds.intersect(manifest.exposureKeysSetIds)
            )
        }
    }

    private suspend fun getLatestAppConfigOrNull(): AppConfig? = withContext(Dispatchers.IO) {
        try {
            api.getAppConfig(api.getManifest().appConfigId)
        } catch (ex: HttpException) {
            Timber.e(ex, "Error getting latest config")
            null
        } catch (ex: IOException) {
            Timber.e(ex, "Error getting latest config")
            null
        }
    }

    private fun createToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, 0)
    }

    private suspend fun getConfigurationFromManifest(manifest: Manifest): ExposureConfiguration {
        val riskCalculationParameters =
            api.getRiskCalculationParameters(manifest.riskCalculationParametersId)
        return ExposureConfiguration.ExposureConfigurationBuilder()
            .setDurationAtAttenuationThresholds(*riskCalculationParameters.durationAtAttenuationThresholds.toIntArray())
            .setMinimumRiskScore(riskCalculationParameters.minimumRiskScore)
            .setTransmissionRiskScores(*riskCalculationParameters.transmissionRiskScores.toIntArray())
            .setDurationScores(*riskCalculationParameters.durationScores.toIntArray())
            .setAttenuationScores(*riskCalculationParameters.attenuationScores.toIntArray())
            .setDaysSinceLastExposureScores(*riskCalculationParameters.daysSinceLastExposureScores.toIntArray())
            .build()
    }

    private suspend fun writeExposureKeyFile(
        id: String,
        responseAsync: Deferred<Response<ResponseBody>>
    ): ExposureKeySet {
        try {
            val response = responseAsync.await()
            return if (response.isSuccessful) {
                try {
                    val file = File(context.cacheDir, id.toByteArray().toByteString().hex())
                    ZipInputStream(response.body()!!.byteStream()).use { input ->
                        validateAndWrite(id, input, file)
                    }
                } catch (ex: IOException) {
                    Timber.w(ex, "Error reading exposure key set")
                    ExposureKeySet(id, null, false)
                }
            } else {
                Timber.e("Error reading exposure key set, response returned ${response.code()}")
                ExposureKeySet(id, null, false)
            }
        } catch (ex: Exception) {
            Timber.e(ex, "Error while processing exposure key set")
            return ExposureKeySet(id, null, false)
        }
    }

    @VisibleForTesting
    internal fun validateAndWrite(id: String, zip: ZipInputStream, file: File): ExposureKeySet {
        var signature: ByteArray? = null

        ZipOutputStream(file.outputStream()).use {
            do {
                val entry = zip.nextEntry ?: break
                if (entry.name == "export.bin" || entry.name == "export.sig") {
                    Timber.d("Reading ${entry.name}")
                    it.putNextEntry(ZipEntry(entry.name))
                    zip.copyTo(it)
                    it.closeEntry()
                } else if (entry.name == "content.sig") {
                    Timber.d("Reading signature")
                    val bos = ByteArrayOutputStream()
                    zip.copyTo(bos)
                    signature = bos.toByteArray()
                }
                zip.closeEntry()
            } while (true)
        }
        return if (signatureValidation) {
            val validator = ResponseSignatureValidator()
            var valid = false
            if (signature != null) {
                ZipInputStream(FileInputStream(file)).use {
                    do {
                        val entry = it.nextEntry ?: break
                        if (entry.name == "export.bin") {
                            try {
                                validator.verifySignature(it, signature!!)
                                valid = true
                            } catch (ex: SignatureValidationException) {
                                Timber.e("File for $id did not pass signature validation")
                            }
                            break
                        }
                        it.closeEntry()
                    } while (true)
                }
            }
            if (!valid) {
                file.delete()
            }
            ExposureKeySet(id, file, valid)
        } else {
            ExposureKeySet(id, file, true)
        }
    }

    suspend fun processManifest(): ProcessManifestResult {
        return withContext(Dispatchers.IO) {
            try {
                val manifest = api.getManifest()
                val keysSuccessful = if (getStatus() == StatusResult.Enabled) {
                    processExposureKeySets(manifest) == ProcessExposureKeysResult.Success
                } else {
                    Timber.w("Cannot process keys, exposure notifications api is disabled")
                    true
                }

                val config = api.getAppConfig(manifest.appConfigId)

                appLifecycleManager.verifyMinimumVersion(config.requiredAppVersionCode)

                if (keysSuccessful) {
                    preferences.edit {
                        putLong(KEY_LAST_KEYS_PROCESSED, clock.millis())
                    }
                }
                // if we are able to fetch the manifest, config etc, then report success
                ProcessManifestResult.Success(config.updatePeriodMinutes)
            } catch (ex: Exception) {
                Timber.e(ex, "Error while processing manifest")
                ProcessManifestResult.Error
            }
        }
    }

    private fun exposureToken(): Flow<String?> = callbackFlow {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                if (key == KEY_LAST_TOKEN_ID) {
                    offer(sharedPreferences.getString(key, null))
                }
            }

        preferences.registerOnSharedPreferenceChangeListener(listener)

        offer(preferences.getString(KEY_LAST_TOKEN_ID, null))

        awaitClose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    /**
     * Return the exposure status
     * @return true if exposures are reported, false otherwise
     */
    fun getLastExposureDate(): Flow<LocalDate?> {
        return exposureToken().distinctUntilChanged().map { token ->
            val hasSummary = token?.let { exposureNotificationsApi.getSummary(it) } != null
            if (hasSummary || (BuildConfig.DEBUG && token == DEBUG_TOKEN)) {
                LocalDate.ofEpochDay(preferences.getLong(KEY_LAST_TOKEN_EXPOSURE_DATE, 0L))
            } else null
        }.onEach { date ->
            if (date == null) {
                resetExposures()
            }
        }.onEach { date ->
            if (date != null) {
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .cancel(ID_EXPOSURE_PUSH_NOTIFICATION)
            }
        }
    }

    fun resetExposures() {
        preferences.edit {
            remove(KEY_LAST_TOKEN_ID)
            remove(KEY_LAST_TOKEN_EXPOSURE_DATE)
        }
    }

    suspend fun addExposure(token: String) {
        val testToken = BuildConfig.DEBUG && token == DEBUG_TOKEN
        val summary = exposureNotificationsApi.getSummary(token)

        Timber.d("New exposure for token $token with summary $summary")

        val currentDaysSinceLastExposure = preferences.getString(KEY_LAST_TOKEN_ID, null)
            ?.let { exposureNotificationsApi.getSummary(it)?.daysSinceLastExposure }
        val newDaysSinceLastExposure = if (testToken) {
            5 // TODO make dynamic from debug screen
        } else {
            summary?.daysSinceLastExposure
        }

        if (!testToken && summary?.matchedKeyCount ?: 0 == 0) {
            Timber.d("Matched key count is 0, ignoring this exposure")
            return
        }

        if (newDaysSinceLastExposure != null &&
            (currentDaysSinceLastExposure == null || newDaysSinceLastExposure < currentDaysSinceLastExposure)
        ) {
            // save new exposure
            preferences.edit {
                putString(KEY_LAST_TOKEN_ID, token)
                putLong(
                    KEY_LAST_TOKEN_EXPOSURE_DATE,
                    LocalDate.now(clock)
                        .minusDays(newDaysSinceLastExposure.toLong()).toEpochDay()
                )
            }
            showNotification(newDaysSinceLastExposure)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "exposure_notifications",
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = context.getString(R.string.notification_channel_description)
            val notificationManager: NotificationManager =
                context.getSystemService(NotificationManager::class.java)!!
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(daysSinceLastExposure: Int) {
        createNotificationChannel()
        val dateOfLastExposure = LocalDate.now(clock)
            .minusDays(daysSinceLastExposure.toLong())

        val pendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_main)
            .setDestination(R.id.nav_post_notification)
            .setArguments(Bundle().apply {
                putLong("epochDayOfLastExposure", dateOfLastExposure.toEpochDay())
            }).createPendingIntent()
        val message = context.getString(
            R.string.notification_message,
            dateOfLastExposure.formatDaysSince(context, clock)
        )
        val builder =
            NotificationCompat.Builder(context, "exposure_notifications")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true) // Do not reveal this notification on a secure lockscreen.
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
        val notificationManager =
            NotificationManagerCompat
                .from(context)
        notificationManager.notify(ID_EXPOSURE_PUSH_NOTIFICATION, builder.build())
    }
}

@VisibleForTesting
internal data class ExposureKeySet(val id: String, val file: File?, val signatureValid: Boolean)

sealed class ProcessExposureKeysResult {
    /**
     * Keys processed successfully
     */
    object Success : ProcessExposureKeysResult()

    /**
     * A server error occurred
     */
    object ServerError : ProcessExposureKeysResult()

    /**
     * Error while processing through the exposure notifications API
     */
    data class ExposureApiError(val diagnosisKeysResult: DiagnosisKeysResult) :
        ProcessExposureKeysResult()

    /**
     * An error occurred
     */
    data class Error(val exception: Exception) : ProcessExposureKeysResult()
}

sealed class ProcessManifestResult {
    data class Success(val nextIntervalMinutes: Int) : ProcessManifestResult()
    object Error : ProcessManifestResult()
}
