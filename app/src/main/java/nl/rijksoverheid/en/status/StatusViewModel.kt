/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.onboarding.OnboardingRepository

class StatusViewModel(private val onboardingRepository: OnboardingRepository) : ViewModel() {
    val headerViewState: LiveData<HeaderViewState> = MutableLiveData(HeaderViewState.Disabled)

    fun hasCompletedOnboarding(): Boolean {
        return onboardingRepository.hasCompletedOnboarding()
    }

    fun getDescription(context: Context, state: HeaderViewState): String = when (state) {
        HeaderViewState.Active -> context.getString(R.string.status_no_exposure_detected_description)
        // TODO format exposure date
        HeaderViewState.Exposed -> context.getString(
            R.string.status_exposure_detected_description,
            "TODO"
        )
        HeaderViewState.Disabled -> context.getString(R.string.status_en_api_disabled_description)
        HeaderViewState.BluetoothDisabled -> context.getString(R.string.status_bluetooth_disabled_description)
    }

    fun onPrimaryActionClicked(state: HeaderViewState) {
        (headerViewState as MutableLiveData).value = HeaderViewState.BluetoothDisabled
    }

    fun onSecondaryActionClicked(state: HeaderViewState) {
    }

    sealed class HeaderViewState(
        @DrawableRes val icon: Int,
        @StringRes val headline: Int,
        @StringRes val primaryAction: Int?,
        @StringRes val secondaryAction: Int?
    ) {
        object Active : HeaderViewState(
            R.drawable.ic_status_no_exposure,
            R.string.status_no_exposure_detected_headline,
            null,
            null
        )

        object Exposed : HeaderViewState(
            R.drawable.ic_status_exposure,
            R.string.status_exposure_detected_headline,
            R.string.status_exposure_what_next,
            R.string.status_reset_exposure
        )

        object Disabled : HeaderViewState(
            R.drawable.ic_status_disabled,
            R.string.status_disabled_headline,
            R.string.status_en_api_disabled_enable,
            null
        )

        object BluetoothDisabled : HeaderViewState(
            R.drawable.ic_status_disabled,
            R.string.status_disabled_headline,
            R.string.status_bluetooth_disabled_enable,
            null
        )
    }
}
