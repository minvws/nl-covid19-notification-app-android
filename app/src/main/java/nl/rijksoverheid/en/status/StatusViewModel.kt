/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.enapi.StatusResult
import nl.rijksoverheid.en.notifier.NotificationsRepository
import nl.rijksoverheid.en.onboarding.OnboardingRepository
import java.time.Clock
import java.time.LocalDate

class StatusViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val exposureNotificationsRepository: ExposureNotificationsRepository,
    private val notificationsRepository: NotificationsRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    fun isPlayServicesUpToDate() = onboardingRepository.isGooglePlayServicesUpToDate()

    val headerState = exposureNotificationsRepository.getStatus().flatMapLatest { status ->
        exposureNotificationsRepository.getLastExposureDate().map { date -> status to date }
    }.map { (status, date) -> createHeaderState(status, date) }
        .onEach {
            notificationsRepository.cancelExposureNotification()
        }
        .asLiveData(viewModelScope.coroutineContext)

    val errorState = combine(
        exposureNotificationsRepository.lastKeyProcessed()
            .flatMapLatest { exposureNotificationsRepository.getStatus() },
        exposureNotificationsRepository.getLastExposureDate(),
        notificationsRepository.exposureNotificationsEnabled(),
    ) { statusResult, localDate, exposureNotificationsEnabled ->
        createErrorState(
            statusResult,
            localDate,
            exposureNotificationsEnabled,
            exposureNotificationsRepository.keyProcessingOverdue()
        )
    }.asLiveData(viewModelScope.coroutineContext)

    val hasSeenLatestTerms = onboardingRepository.hasSeenLatestTerms()
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

    private fun createErrorState(
        status: StatusResult,
        date: LocalDate?,
        exposureNotificationsEnabled: Boolean,
        keyProcessingOverdue: Boolean
    ): ErrorState =
        if (status != StatusResult.Enabled && date != null) {
            ErrorState.ConsentRequired
        } else if (!exposureNotificationsEnabled) {
            ErrorState.NotificationsDisabled
        } else if (keyProcessingOverdue) {
            ErrorState.SyncIssues
        } else {
            ErrorState.None
        }

    fun removeExposure() {
        viewModelScope.launch {
            exposureNotificationsRepository.resetExposures()
        }
    }

    fun resetErrorState() {
        viewModelScope.launch {
            exposureNotificationsRepository.resetLastKeysProcessed()
        }
    }

    fun setHasSeenLatestTerms() {
        onboardingRepository.setHasSeenLatestTerms()
    }

    sealed class HeaderState {
        object Active : HeaderState()
        object Disabled : HeaderState()
        data class Exposed(val date: LocalDate, val clock: Clock) : HeaderState()
    }

    sealed class ErrorState {
        object None : ErrorState()
        object ConsentRequired : ErrorState()
        object NotificationsDisabled : ErrorState()
        object SyncIssues : ErrorState()
    }
}
