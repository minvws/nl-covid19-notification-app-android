/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import nl.rijksoverheid.en.api.ExposureNotificationService
import nl.rijksoverheid.en.api.model.TemporaryExposureKey
import nl.rijksoverheid.en.enapi.ExposureNotificationApi
import nl.rijksoverheid.en.enapi.EnableNotificationsResult
import nl.rijksoverheid.en.enapi.StatusResult
import nl.rijksoverheid.en.enapi.DisableNotificationsResult
import nl.rijksoverheid.en.enapi.TemporaryExposureKeysResult
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.security.SecureRandom

private val EQUAL_WEIGHTS = intArrayOf(1, 1, 1, 1, 1, 1, 1, 1)
private const val KEY_TOKENS = "tokens"

class ExposureNotificationsRepository(
    private val context: Context,
    private val exposureNotificationsApi: ExposureNotificationApi,
    private val api: ExposureNotificationService,
    private val exposures: SharedPreferences
) {

    suspend fun requestEnableNotifications(): EnableNotificationsResult {
        return exposureNotificationsApi.requestEnableNotifications()
    }

    suspend fun requestDisableNotifications(): DisableNotificationsResult {
        return exposureNotificationsApi.disableNotifications()
    }

    suspend fun getStatus(): StatusResult {
        return exposureNotificationsApi.getStatus()
    }

    suspend fun exportTemporaryExposureKeys(): ExportTemporaryExposureKeysResult {
        val result = exposureNotificationsApi.requestTemporaryExposureKeyHistory()
        Timber.d("Result = $result")

        when (result) {
            is TemporaryExposureKeysResult.Success -> {
                if (result.keys.isNotEmpty()) {
                    try {
                        uploadKeys(result.keys)
                    } catch (ex: Exception) {
                        Timber.e(ex, "Error uploading keys")
                        return ExportTemporaryExposureKeysResult.Error(ex)
                    }
                }
                return ExportTemporaryExposureKeysResult.Success(result.keys.size)
            }
            is TemporaryExposureKeysResult.RequireConsent -> return ExportTemporaryExposureKeysResult.RequireConsent(
                result.resolution
            )
            is TemporaryExposureKeysResult.UnknownError -> return ExportTemporaryExposureKeysResult.Error(
                result.exception
            )
        }
    }

    suspend fun importTemporaryExposureKeys(): ImportTemporaryExposureKeysResult =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getExposureKeysFile()
                if (!response.isSuccessful) {
                    // TODO handle this in a better way
                    return@withContext ImportTemporaryExposureKeysResult.Error(RuntimeException("error code ${response.code()}"))
                }
                val keys = response.body()!!.bytes()
                val token = generateImportToken()
                val importFile = File(context.cacheDir, token)
                importFile.outputStream().use {
                    it.write(keys)
                }
                exposureNotificationsApi.provideDiagnosisKeys(
                    listOf(importFile),
                    ExposureConfiguration.ExposureConfigurationBuilder()
                        .setAttenuationScores(*EQUAL_WEIGHTS)
                        .setAttenuationWeight(1)
                        .setDaysSinceLastExposureScores(*EQUAL_WEIGHTS)
                        .setDaysSinceLastExposureWeight(1)
                        .setDurationScores(*EQUAL_WEIGHTS)
                        .setDurationWeight(1)
                        .setTransmissionRiskScores(*EQUAL_WEIGHTS)
                        .setTransmissionRiskWeight(1)
                        .setMinimumRiskScore(1)
                        .build(),
                    token
                )
                ImportTemporaryExposureKeysResult.Success
            } catch (ex: IOException) {
                Timber.e(ex, "Error downloading and processing keys")
                ImportTemporaryExposureKeysResult.Error(ex)
            }
        }

    private fun generateImportToken(): String {
        val tokenBytes = ByteArray(32)
        SecureRandom().nextBytes(tokenBytes)
        return Base64.encodeToString(tokenBytes, Base64.NO_WRAP or Base64.NO_PADDING)
            .replace("/", "_").replace("+", "-")
    }

    private suspend fun uploadKeys(keys: List<com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey>) {
        val request = keys.map {
            TemporaryExposureKey(
                it.keyData,
                it.rollingStartIntervalNumber,
                it.rollingPeriod,
                1
            )
        }
        withContext(Dispatchers.IO) {
            api.postTempExposureKeys(request)
        }
    }

    private fun exposureTokens(): Flow<Set<String>> = callbackFlow {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                if (key == KEY_TOKENS) {
                    offer(
                        sharedPreferences.getStringSet(key, emptySet<String>())
                            ?: emptySet<String>()
                    )
                }
            }

        exposures.registerOnSharedPreferenceChangeListener(listener)

        offer(exposures.getStringSet(KEY_TOKENS, emptySet<String>()) ?: emptySet<String>())

        awaitClose {
            exposures.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    /**
     * Return the exposure status
     * @return true if exposures are reported, false otherwise
     */
    fun isExposureDetected(): Flow<Boolean> {
        return exposureTokens().distinctUntilChanged().map {
            val activeTokens = mutableSetOf<String>()
            for (token in it) {
                val exposure = exposureNotificationsApi.getSummary(token)
                if (exposure != null) {
                    activeTokens.add(token)
                }
            }

            activeTokens
        }.onEach {
            exposures.edit {
                putStringSet(KEY_TOKENS, it)
            }
        }.map {
            it.isNotEmpty()
        }
    }

    fun resetExposures() {
        exposures.edit {
            putStringSet(KEY_TOKENS, emptySet())
        }
    }

    fun addExposure(token: String) {
        Timber.d("New exposure for token $token")
        val tokens = exposures.getStringSet(KEY_TOKENS, emptySet()) ?: emptySet()
        if (!tokens.contains(token)) {
            val newTokens = tokens.toMutableSet().apply {
                add(token)
            }
            exposures.edit { putStringSet(KEY_TOKENS, newTokens) }
        }
    }
}

sealed class ExportTemporaryExposureKeysResult {
    data class Success(val numKeysExported: Int) : ExportTemporaryExposureKeysResult()
    data class RequireConsent(val resolution: PendingIntent) : ExportTemporaryExposureKeysResult()
    data class Error(val exception: Exception) : ExportTemporaryExposureKeysResult()
}

sealed class ImportTemporaryExposureKeysResult {
    object Success : ImportTemporaryExposureKeysResult()
    data class Error(val exception: Exception) : ImportTemporaryExposureKeysResult()
}
