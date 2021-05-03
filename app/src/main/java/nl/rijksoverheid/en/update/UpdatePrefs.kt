/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.update

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class UpdatePrefs(
    context: Context,
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(
        context
    )
) {
    var lastVersionUpdated: Int
        get() = preferences.getInt(KEY_UPDATED_VERSION, 0)
        set(value) {
            preferences.edit {
                putInt(KEY_UPDATED_VERSION, value)
            }
        }

    companion object {
        private const val KEY_UPDATED_VERSION = "last_updated_version"
    }
}
