/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.enapi.StatusResult
import nl.rijksoverheid.en.lifecyle.Event
import nl.rijksoverheid.en.onboarding.OnboardingRepository
import java.time.Clock
import java.time.LocalDate

class StatusViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val notificationsRepository: ExposureNotificationsRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    val requestEnableNotifications: LiveData<Event<Unit>> = MutableLiveData()

    fun isPlayServicesUpToDate() = onboardingRepository.isGooglePlayServicesUpToDate()

    val headerState = notificationsRepository.getStatus().flatMapLatest { status ->
        notificationsRepository.getLastExposureDate().map { date -> status to date }
    }.map { (status, date) -> createHeaderState(status, date) }
        .asLiveData(viewModelScope.coroutineContext)

    val errorState = notificationsRepository.getStatus().flatMapLatest { status ->
        notificationsRepository.getLastExposureDate().map { date -> status to date }
    }.map { (status, date) -> createErrorState(status, date) }
        .asLiveData(viewModelScope.coroutineContext)

    fun hasCompletedOnboarding(): Boolean {
        return onboardingRepository.hasCompletedOnboarding()
    }

    private fun createHeaderState(status: StatusResult, date: LocalDate?): HeaderState {
        return when {
            date != null -> HeaderState.Exposed(date, clock)
            status is StatusResult.Enabled -> HeaderState.Active
            else -> HeaderState.Disabled
        }
    }

    private fun createErrorState(status: StatusResult, date: LocalDate?): ErrorState =
        if (status != StatusResult.Enabled && date != null) {
            ErrorState.ConsentRequired
        } else if (notificationsRepository.keyProcessingOverdue) {
            ErrorState.SyncIssues
        } else {
            ErrorState.None
        }

    fun removeExposure() {
        notificationsRepository.resetExposures()
    }

    fun resetErrorState() {
        viewModelScope.launch {
            notificationsRepository.requestEnableNotifications()
        }
    }

    sealed class HeaderState {
        object Active : HeaderState()
        object Disabled : HeaderState()
        data class Exposed(val date: LocalDate, val clock: Clock) : HeaderState()
    }

    sealed class ErrorState {
        object None : ErrorState()
        object ConsentRequired : ErrorState()
        object SyncIssues : ErrorState()
    }
}
