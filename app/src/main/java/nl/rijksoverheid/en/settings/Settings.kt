/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * Thin wrapper around the default app preferences
 */
class Settings(
    context: Context,
    preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
) {
    val checkOnWifiOnly: Boolean = preferences.getBoolean(KEY_WIFI_ONLY, false)

    companion object {
        const val KEY_WIFI_ONLY = "wifi_only"
    }
}
