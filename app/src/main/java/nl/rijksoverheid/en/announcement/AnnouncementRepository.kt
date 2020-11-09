/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.announcement

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val KEY_INTEROP_ANNOUNCEMENT = "interop_announcement"

class AnnouncementRepository(
    private val sharedPreferences: SharedPreferences
) {

    fun hasSeenInteropAnnouncement(): Flow<Boolean> = callbackFlow {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                if (key == KEY_INTEROP_ANNOUNCEMENT) {
                    offer(sharedPreferences.getBoolean(key, false))
                }
            }

        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

        offer(sharedPreferences.getBoolean(KEY_INTEROP_ANNOUNCEMENT, false))

        awaitClose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    fun setHasSeenInteropAnnouncement(value: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_INTEROP_ANNOUNCEMENT, value) }
    }
}
