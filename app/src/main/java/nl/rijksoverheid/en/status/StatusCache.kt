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

private const val KEY_CACHED_STATUS = "cached_status"

open class StatusCache(private val preferences: SharedPreferences) {

    fun updateCachedStatus(cachedStatus: CachedStatus) = preferences.edit {
        putInt(KEY_CACHED_STATUS, cachedStatus.ordinal)
    }

    fun getCachedStatus(): Flow<CachedStatus> = callbackFlow<CachedStatus> {
        // Emit the current cached value, fallback to Disabled
        offer(getCachedStatusFromPreferences(preferences))
        // Emit cached value whenever it changes
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                if (key == KEY_CACHED_STATUS) {
                    offer(getCachedStatusFromPreferences(sharedPreferences))
                }
            }

        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.distinctUntilChanged()

    private fun getCachedStatusFromPreferences(preferences: SharedPreferences): CachedStatus {
        val cachedStatus = preferences.getInt(KEY_CACHED_STATUS, CachedStatus.NONE.ordinal)
        return CachedStatus.values()[cachedStatus]
    }

    enum class CachedStatus { ENABLED, INVALID_PRECONDITIONS, DISABLED, NONE }
}
