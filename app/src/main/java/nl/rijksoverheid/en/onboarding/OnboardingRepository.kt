/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.onboarding

import android.content.SharedPreferences
import androidx.core.content.edit

private const val KEY_COMPLETED_ONBOARDING = "completed_onboarding"

typealias GooglePlayServicesUpToDateChecker = () -> Boolean

class OnboardingRepository(
    private val sharedPreferences: SharedPreferences,
    private val googlePlayServicesUpToDateChecker: GooglePlayServicesUpToDateChecker
) {

    fun hasCompletedOnboarding(): Boolean {
        return sharedPreferences.getBoolean(KEY_COMPLETED_ONBOARDING, false)
    }

    fun setHasCompletedOnboarding(value: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_COMPLETED_ONBOARDING, value) }
    }

    fun isGooglePlayServicesUpToDate(): Boolean = googlePlayServicesUpToDateChecker()
}
