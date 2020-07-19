/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import nl.rijksoverheid.en.lifecyle.Event

class OnboardingViewModel(private val repository: OnboardingRepository) : ViewModel() {
    val onboardingComplete: LiveData<Event<Unit>> = MutableLiveData()
    val skipConsentConfirmation: LiveData<Event<Unit>> = MutableLiveData()

    fun isGooglePlayServicesUpToDate() = repository.isGooglePlayServicesUpToDate()

    fun finishOnboarding() {
        repository.setHasCompletedOnboarding(true)
        (onboardingComplete as MutableLiveData).value = Event(Unit)
    }

    fun skipConsent() {
        (skipConsentConfirmation as MutableLiveData).value = Event(Unit)
    }
}
