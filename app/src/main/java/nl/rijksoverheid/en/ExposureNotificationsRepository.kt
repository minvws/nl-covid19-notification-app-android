/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.location.LocationManager
import android.os.Build
import android.util.Base64
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import nl.rijksoverheid.en.api.CdnService
import nl.rijksoverheid.en.api.model.AppConfig
import nl.rijksoverheid.en.api.model.Manifest
import nl.rijksoverheid.en.applifecycle.AppLifecycleManager
import nl.rijksoverheid.en.config.AppConfigManager
import nl.rijksoverheid.en.enapi.DiagnosisKeysResult
import nl.rijksoverheid.en.enapi.DisableNotificationsResult
import nl.rijksoverheid.en.enapi.EnableNotificationsResult
import nl.rijksoverheid.en.enapi.StatusResult
import nl.rijksoverheid.en.enapi.nearby.ExposureNotificationApi
import nl.rijksoverheid.en.job.BackgroundWorkScheduler
import nl.rijksoverheid.en.lifecyle.asFlow
import nl.rijksoverheid.en.signing.ResponseSignatureValidator
import nl.rijksoverheid.en.signing.SignatureValidationException
import nl.rijksoverheid.en.status.StatusCache
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
import java.time.Period
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import nl.rijksoverheid.en.api.BuildConfig as ApiBuildConfig

private const val KEY_LAST_TOKEN_ID = "last_token_id"
private const val KEY_LAST_TOKEN_EXPOSURE_DATE = "last_token_exposure_date"
private const val KEY_EXPOSURE_KEY_SETS = "exposure_key_sets"
private const val KEY_LAST_KEYS_PROCESSED = "last_keys_processed"
private const val KEY_PROCESSING_OVERDUE_THRESHOLD_MINUTES = 24 * 60
private const val KEY_MIN_RISK_SCORE = "min_risk_score"

class ExposureNotificationsRepository(
    private val context: Context,
    private val exposureNotificationsApi: ExposureNotificationApi,
    private val api: CdnService,
    private val preferences: SharedPreferences,
    private val manifestWorkerScheduler: BackgroundWorkScheduler,
    private val appLifecycleManager: AppLifecycleManager,
    private val statusCache: StatusCache,
    private val appConfigManager: AppConfigManager,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val signatureValidation: Boolean = ApiBuildConfig.FEATURE_RESPONSE_SIGNATURES,
    lifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get()
) {
    companion object {
        const val DEBUG_TOKEN = "TEST-TOKEN"
    }

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
                putLong(KEY_LAST_KEYS_PROCESSED, clock.millis())
            }
            val interval = appConfigManager.getCachedConfigOrDefault().updatePeriodMinutes
            manifestWorkerScheduler.schedule(interval)
            statusCache.updateCachedStatus(StatusCache.CachedStatus.ENABLED)
        }
        return result
    }

    fun resetLastKeysProcessed() {
        preferences.edit {
            // reset the timer
            putLong(KEY_LAST_KEYS_PROCESSED, clock.millis())
        }
    }

    suspend fun requestEnableNotificationsForcingConsent(): EnableNotificationsResult {
        exposureNotificationsApi.disableNotifications()
        return requestEnableNotifications()
    }

    private suspend fun requestDisableNotifications(): DisableNotificationsResult {
        manifestWorkerScheduler.cancel()
        preferences.edit {
            remove(KEY_LAST_KEYS_PROCESSED)
        }
        val result = exposureNotificationsApi.disableNotifications()
        if (result == DisableNotificationsResult.Disabled) {
            statusCache.updateCachedStatus(StatusCache.CachedStatus.DISABLED)
        }
        return result
    }

    private val refreshOnStart = lifecycleOwner.asFlow().filter { it == Lifecycle.State.STARTED }
        .map { Unit }.onStart { emit(Unit) }

    // Triggers on subscribe and any changes to bluetooth / location permission state
    private val preconditionsChanged = callbackFlow<Unit> {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                offer(Unit)
            }
        }
        val filter = IntentFilter().apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                addAction(LocationManager.MODE_CHANGED_ACTION)
            }
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        context.registerReceiver(receiver, filter)

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }.onStart { emit(Unit) }

    // Triggers on subscribe and any changes to bluetooth / location permission state or cached state
    private val triggers = refreshOnStart.flatMapLatest { preconditionsChanged }
        .flatMapLatest { statusCache.getCachedStatus() }

    /**
     * Directly emits a cached [StatusResult] from cache, followed up by the up to date value from
     * the API. Any updates to the permissions and framework state will lead to new values being
     * emitted.
     */
    fun getStatus(): Flow<StatusResult> = triggers.flatMapLatest {
        flow {
            // Synchronously emit the cached status
            emit(
                when (it) {
                    StatusCache.CachedStatus.ENABLED -> StatusResult.Enabled
                    StatusCache.CachedStatus.INVALID_PRECONDITIONS -> StatusResult.InvalidPreconditions
                    StatusCache.CachedStatus.DISABLED -> StatusResult.Disabled
                    StatusCache.CachedStatus.NONE -> StatusResult.Disabled
                }
            )
            // Asynchronously emit the up to date status
            emit(getCurrentStatus())
        }
    }.distinctUntilChanged()

    suspend fun getCurrentStatus(): StatusResult {
        val result = exposureNotificationsApi.getStatus()
        return if (result == StatusResult.Enabled) {
            if (isBluetoothEnabled() && isLocationPreconditionSatisfied()) {
                statusCache.updateCachedStatus(StatusCache.CachedStatus.ENABLED)
                StatusResult.Enabled
            } else {
                statusCache.updateCachedStatus(StatusCache.CachedStatus.INVALID_PRECONDITIONS)
                StatusResult.InvalidPreconditions
            }
        } else {
            if (result == StatusResult.Disabled) {
                statusCache.updateCachedStatus(StatusCache.CachedStatus.DISABLED)
            }
            result
        }
    }

    private fun isBluetoothEnabled(): Boolean {
        val manager = context.getSystemService(BluetoothManager::class.java) ?: return false
        return manager.adapter.isEnabled
    }

    /**
     * Check the location manager to see if location is enabled.
     * @return false if location is not enabled, true if the [LocationManager] service is null or if running on Android R and up
     */
    private fun isLocationPreconditionSatisfied(): Boolean {
        return context.getSystemService(LocationManager::class.java)
            ?.let { LocationManagerCompat.isLocationEnabled(it) || Build.VERSION.SDK_INT > Build.VERSION_CODES.Q }
            ?: true
    }

    /**
     * Downloads new exposure key sets from the server and processes them
     */
    @VisibleForTesting
    internal suspend fun processExposureKeySets(manifest: Manifest): ProcessExposureKeysResult {
        if (exposureNotificationsApi.getStatus() == StatusResult.Disabled) {
            Timber.d("Exposure notifications api is disabled")
            // we don't consider this an error, the user might have disabled it from the system settings.
            return ProcessExposureKeysResult.Success
        }

        val processedSets = preferences.getStringSet(KEY_EXPOSURE_KEY_SETS, emptySet())!!
        val updates = manifest.exposureKeysSetIds.toMutableSet().apply {
            removeAll(processedSets)
        }
        val files = updates.map {
            it to supervisorScope { async { api.getExposureKeySetFile(it) } }
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
            Timber.d("No files to process")
            // shortcut if nothing to process
            return if (!hasErrors) {
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
            return ProcessExposureKeysResult.Error(ex)
        }

        Timber.d("Processing ${validFiles.size} files")

        preferences.edit {
            putInt(KEY_MIN_RISK_SCORE, configuration.minimumRiskScore)
        }

        val result = exposureNotificationsApi.provideDiagnosisKeys(
            validFiles.map { it.file!! },
            configuration,
            createToken()
        )
        return when (result) {
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

                val config = try {
                    api.getAppConfig(manifest.appConfigId)
                } catch (ex: HttpException) {
                    Timber.w(ex, "Could not fetch config, returning default")
                    AppConfig()
                } catch (ex: IOException) {
                    Timber.w(ex, "Could not fetch config, returning default")
                    AppConfig()
                }

                if (config.deactivated) {
                    Timber.d("App is deactivated")
                    requestDisableNotifications()
                    return@withContext ProcessManifestResult.Disabled
                }

                val result = processExposureKeySets(manifest)
                Timber.d("Processing keys result = $result")

                appLifecycleManager.verifyMinimumVersion(config.requiredAppVersionCode, true)

                if (result == ProcessExposureKeysResult.Success) {
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

    fun lastKeyProcessed(): Flow<Long?> = callbackFlow {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                if (key == KEY_LAST_KEYS_PROCESSED) {
                    offer(sharedPreferences.getLong(key, 0))
                }
            }

        preferences.registerOnSharedPreferenceChangeListener(listener)

        offer(preferences.getLong(KEY_LAST_KEYS_PROCESSED, 0))

        awaitClose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    /**
     * Return the exposure status
     * @return true if exposures are reported, false otherwise
     */
    fun getLastExposureDate(): Flow<LocalDate?> {
        return exposureToken().distinctUntilChanged().map {
            val timestamp = preferences.getLong(KEY_LAST_TOKEN_EXPOSURE_DATE, 0L)
            if (timestamp > 0) {
                LocalDate.ofEpochDay(timestamp)
            } else {
                null
            }
        }
    }

    fun resetExposures() {
        preferences.edit {
            // Use putString instead of remove, otherwise encrypted shared preferences don't call
            // an associated shared preferences listener.
            putString(KEY_LAST_TOKEN_ID, null)
            putString(KEY_LAST_TOKEN_EXPOSURE_DATE, null)
        }
    }

    suspend fun getDaysSinceLastExposure(): Int? {
        val date = getLastExposureDate().first()
        return date?.let {
            Period.between(date, LocalDate.now(clock)).days
        }
    }

    suspend fun addExposure(token: String): AddExposureResult {
        val testToken = BuildConfig.FEATURE_DEBUG_NOTIFICATION && token == DEBUG_TOKEN
        val summary = exposureNotificationsApi.getSummary(token)

        Timber.d("New exposure for token $token with summary $summary")

        val currentDaysSinceLastExposure = preferences.getString(KEY_LAST_TOKEN_ID, null)
            ?.let { exposureNotificationsApi.getSummary(it)?.daysSinceLastExposure }
        val newDaysSinceLastExposure = if (testToken) {
            5 // TODO make dynamic from debug screen
        } else {
            summary?.daysSinceLastExposure
        }

        val minRiskScore = preferences.getInt(KEY_MIN_RISK_SCORE, 0)

        if (!testToken && (summary?.matchedKeyCount ?: 0 == 0 || summary?.maximumRiskScore ?: 0 < minRiskScore)) {
            Timber.d("Exposure has no matches or does not meet required risk score")
            return AddExposureResult.Processed
        }

        return if (newDaysSinceLastExposure != null &&
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
            AddExposureResult.Notify(newDaysSinceLastExposure)
        } else {
            AddExposureResult.Processed
        }
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
    object Disabled : ProcessManifestResult()
    object Error : ProcessManifestResult()
}

sealed class AddExposureResult {
    object Processed : AddExposureResult()
    data class Notify(val daysSinceExposure: Int) : AddExposureResult()
}
