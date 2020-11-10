/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.onboarding

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val KEY_COMPLETED_ONBOARDING = "completed_onboarding"
private const val KEY_TERMS_VERSION = "terms_version"

private const val TERMS_VERSION = 2

typealias GooglePlayServicesUpToDateChecker = () -> Boolean

class OnboardingRepository(
    private val sharedPreferences: SharedPreferences,
    private val googlePlayServicesUpToDateChecker: GooglePlayServicesUpToDateChecker
) {

    fun hasCompletedOnboarding(): Boolean {
        return sharedPreferences.getBoolean(KEY_COMPLETED_ONBOARDING, false)
    }

    fun setHasCompletedOnboarding(value: Boolean) {
        sharedPreferences.edit {
            putBoolean(KEY_COMPLETED_ONBOARDING, value)
            putInt(KEY_TERMS_VERSION, TERMS_VERSION)
        }
    }

    fun isGooglePlayServicesUpToDate(): Boolean = googlePlayServicesUpToDateChecker()

    fun hasSeenLatestTerms(): Flow<Boolean> = callbackFlow {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                if (key == KEY_TERMS_VERSION) {
                    offer(sharedPreferences.getInt(key, 1) == TERMS_VERSION)
                }
            }

        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

        offer(sharedPreferences.getInt(KEY_TERMS_VERSION, 1) == TERMS_VERSION)

        awaitClose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    fun setHasSeenLatestTerms() {
        sharedPreferences.edit { putInt(KEY_TERMS_VERSION, TERMS_VERSION) }
    }
}
