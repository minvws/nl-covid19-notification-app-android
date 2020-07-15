/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

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

    fun getCachedStatus(): Flow<CachedStatus> = callbackFlow {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                if (key == KEY_CACHED_STATUS) offer(
                    CachedStatus.values()[sharedPreferences.getInt(
                        KEY_CACHED_STATUS,
                        0
                    )]
                )
            }
        preferences.registerOnSharedPreferenceChangeListener(listener)

        offer(CachedStatus.values()[preferences.getInt(KEY_CACHED_STATUS, 0)])

        awaitClose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.distinctUntilChanged()

    enum class CachedStatus { ENABLED, INVALID_PRECONDITIONS, DISABLED }
}
