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
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.lifecyle.Event

class OnboardingViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val exposureNotificationsRepository: ExposureNotificationsRepository
) : ViewModel() {
    val onboardingComplete: LiveData<Event<Unit>> = MutableLiveData()
    val continueOnboarding: LiveData<Event<Unit>> = MutableLiveData()
    val skipConsentConfirmation: LiveData<Event<Unit>> = MutableLiveData()
    val privacyPolicyConsentGiven: LiveData<Boolean> = MutableLiveData(false)

    private val _isExposureNotificationApiUpToDate: MutableLiveData<Boolean> = MutableLiveData()
    val isExposureNotificationApiUpToDate: LiveData<Boolean> = _isExposureNotificationApiUpToDate

    fun finishOnboarding() {
        if (privacyPolicyConsentGiven.value != true)
            return

        onboardingRepository.setHasCompletedOnboarding(true)
        (onboardingComplete as MutableLiveData).value = Event(Unit)
    }

    fun skipConsent() {
        (skipConsentConfirmation as MutableLiveData).value = Event(Unit)
    }

    fun togglePrivacyPolicyConsent() {
        (privacyPolicyConsentGiven as MutableLiveData).value = !(privacyPolicyConsentGiven.value ?: false)
    }

    fun continueOnboarding() {
        (continueOnboarding as MutableLiveData).value = Event(Unit)
    }

    fun refreshExposureNotificationApiUpToDate() {
        viewModelScope.launch {
            _isExposureNotificationApiUpToDate.value = onboardingRepository.isGooglePlayServicesUpToDate() &&
                !exposureNotificationsRepository.isExposureNotificationApiUpdateRequired()
        }
    }
}
