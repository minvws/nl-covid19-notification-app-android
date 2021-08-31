/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

private const val KEY_CACHED_STATUS_NAME = "cached_status_name"

open class StatusCache(private val preferences: SharedPreferences) {

    fun updateCachedStatus(cachedStatus: CachedStatus) = preferences.edit {
        putString(KEY_CACHED_STATUS_NAME, cachedStatus.name)
    }

    fun getCachedStatus(): Flow<CachedStatus> = callbackFlow {
        // Emit the current cached value, fallback to Disabled
        trySend(getCachedStatusFromPreferences(preferences))
        // Emit cached value whenever it changes
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                if (key == KEY_CACHED_STATUS_NAME) {
                    trySend(getCachedStatusFromPreferences(sharedPreferences))
                }
            }

        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.distinctUntilChanged()

    private fun getCachedStatusFromPreferences(preferences: SharedPreferences): CachedStatus {
        val cachedStatusName = preferences.getString(KEY_CACHED_STATUS_NAME, CachedStatus.NONE.name)
        return CachedStatus.valueOf(cachedStatusName ?: CachedStatus.NONE.name)
    }

    enum class CachedStatus {
        ENABLED,
        BLUETOOTH_DISABLED,
        LOCATION_PRECONDITION_NOT_SATISFIED,
        DISABLED,
        NONE
    }
}
