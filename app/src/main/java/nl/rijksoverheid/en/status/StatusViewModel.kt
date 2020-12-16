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
import nl.rijksoverheid.en.config.AppConfigManager
import nl.rijksoverheid.en.enapi.StatusResult
import nl.rijksoverheid.en.notifier.NotificationsRepository
import nl.rijksoverheid.en.onboarding.OnboardingRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class StatusViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val exposureNotificationsRepository: ExposureNotificationsRepository,
    private val notificationsRepository: NotificationsRepository,
    private val appConfigManager: AppConfigManager,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    fun isPlayServicesUpToDate() = onboardingRepository.isGooglePlayServicesUpToDate()

    val headerState = combine(
        exposureNotificationsRepository.getStatus(),
        exposureNotificationsRepository.lastKeyProcessed(),
        exposureNotificationsRepository.notificationsEnabledTimestamp()
    ) { status, _, _ ->
        status
    }.flatMapLatest { status ->
        exposureNotificationsRepository.getLastExposureDate().map { date -> status to date }
    }.map { (status, date) ->
        createHeaderState(status, date, exposureNotificationsRepository.keyProcessingOverdue())
    }.onEach {
        notificationsRepository.cancelExposureNotification()
    }.asLiveData(viewModelScope.coroutineContext)

    val exposureDetected: Boolean
        get() = headerState.value is HeaderState.Exposed

    val errorState = combine(
        exposureNotificationsRepository.notificationsEnabledTimestamp()
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

    val lastKeysProcessed = exposureNotificationsRepository.lastKeyProcessed()
        .map {
            if (it != null && it > 0)
                LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
            else
                null
        }.asLiveData(viewModelScope.coroutineContext)

    suspend fun getAppointmentPhoneNumber() =
        appConfigManager.getCachedConfigOrDefault().appointmentPhoneNumber

    fun hasCompletedOnboarding(): Boolean {
        return onboardingRepository.hasCompletedOnboarding()
    }

    private fun createHeaderState(
        status: StatusResult,
        date: LocalDate?,
        keyProcessingOverdue: Boolean
    ): HeaderState {
        return when {
            date != null -> HeaderState.Exposed(date, clock)
            status !is StatusResult.Enabled -> HeaderState.Disabled
            keyProcessingOverdue -> HeaderState.SyncIssues
            else -> HeaderState.Active
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
        } else if (date != null && keyProcessingOverdue) {
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
            exposureNotificationsRepository.rescheduleBackgroundJobs()
        }
    }

    fun setHasSeenLatestTerms() {
        onboardingRepository.setHasSeenLatestTerms()
    }

    sealed class HeaderState {
        object Active : HeaderState()
        object Disabled : HeaderState()
        object SyncIssues : HeaderState()
        data class Exposed(val date: LocalDate, val clock: Clock) : HeaderState()
    }

    sealed class ErrorState {
        object None : ErrorState()
        object ConsentRequired : ErrorState()
        object NotificationsDisabled : ErrorState()
        object SyncIssues : ErrorState()
    }
}
