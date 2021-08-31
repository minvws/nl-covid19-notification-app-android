/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.LocalDateTime
import java.time.ZoneOffset

private const val KEY_WIFI_ONLY = "wifi_only"
private const val KEY_PAUSED = "paused"
private const val KEY_PAUSED_UNTIL = "paused_until"
private const val KEY_PAUSE_CONFIRMATION = "paused_confirmation"
private const val KEY_APP_LANGUAGE_DUTCH = "app_language_dutch"

/**
 * Thin wrapper around the default app preferences
 */
class Settings(
    context: Context,
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(
        context
    )
) {
    var checkOnWifiOnly: Boolean
        get() = preferences.getBoolean(KEY_WIFI_ONLY, false)
        set(value) {
            preferences.edit {
                putBoolean(KEY_WIFI_ONLY, value)
            }
        }

    val exposureStatePausedState: PausedState
        get() = if (preferences.getBoolean(KEY_PAUSED, false)) {
            PausedState.Paused(
                LocalDateTime.ofEpochSecond(
                    preferences.getLong(KEY_PAUSED_UNTIL, 0),
                    0,
                    ZoneOffset.UTC
                )
            )
        } else {
            PausedState.Enabled
        }

    var skipPauseConfirmation: Boolean
        get() = preferences.getBoolean(KEY_PAUSE_CONFIRMATION, false)
        set(value) {
            preferences.edit {
                putBoolean(KEY_PAUSE_CONFIRMATION, value)
            }
        }

    fun setExposureNotificationsPaused(until: LocalDateTime) {
        preferences.edit {
            putBoolean(KEY_PAUSED, true)
            putLong(KEY_PAUSED_UNTIL, until.toEpochSecond(ZoneOffset.UTC))
        }
    }

    fun clearExposureNotificationsPaused() {
        preferences.edit {
            remove(KEY_PAUSED)
            remove(KEY_PAUSED_UNTIL)
        }
    }

    var isAppSetToDutch: Boolean
        get() = preferences.getBoolean(KEY_APP_LANGUAGE_DUTCH, false)
        set(value) = preferences.edit { putBoolean(KEY_APP_LANGUAGE_DUTCH, value) }

    fun observeChanges(): Flow<Settings> = callbackFlow {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(this@Settings) }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(this@Settings)
        awaitClose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    sealed class PausedState {
        object Enabled : PausedState()
        data class Paused(val pausedUntil: LocalDateTime) : PausedState()
    }
}
