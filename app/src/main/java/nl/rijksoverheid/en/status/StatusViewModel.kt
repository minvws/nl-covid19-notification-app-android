/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
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

    private val refreshStatus = MutableLiveData(Unit)

    val requestEnableNotifications: LiveData<Event<Unit>> = MutableLiveData()
    val confirmRemoveExposedMessage: LiveData<Event<Unit>> = MutableLiveData()
    val navigateToPostNotification: LiveData<Event<LocalDate>> = MutableLiveData()

    fun isPlayServicesUpToDate() = onboardingRepository.isGooglePlayServicesUpToDate()

    val headerState = refreshStatus.switchMap {
        liveData {
            emit(notificationsRepository.getStatus())
        }.switchMap { status ->
            notificationsRepository.getLastExposureDate()
                .asLiveData(viewModelScope.coroutineContext)
                .map { date -> status to date }
        }.map { (status, date) -> createHeaderState(status, date) }
    }

    val errorState = refreshStatus.switchMap {
        liveData {
            emit(notificationsRepository.getStatus())
        }.switchMap { status ->
            notificationsRepository.getLastExposureDate()
                .asLiveData(viewModelScope.coroutineContext)
                .map { date -> status to date }
        }.map { (status, date) -> createErrorState(status, date) }
    }.startWith(ErrorState.None)

    fun hasCompletedOnboarding(): Boolean {
        return onboardingRepository.hasCompletedOnboarding()
    }

    fun refreshStatus() {
        refreshStatus.value = Unit
    }

    private fun createHeaderState(status: StatusResult, date: LocalDate?): HeaderState =
        when {
            date != null -> HeaderState.Exposed(
                date,
                clock,
                { (navigateToPostNotification as MutableLiveData).value = Event(date) },
                { (confirmRemoveExposedMessage as MutableLiveData).value = Event(Unit) }
            )
            status is StatusResult.Enabled -> HeaderState.Active
            else -> HeaderState.Disabled { resetAndRequestEnableNotifications() }
        }

    private fun createErrorState(status: StatusResult, date: LocalDate?): ErrorState =
        if (status != StatusResult.Enabled && date != null) {
            ErrorState.ConsentRequired {
                viewModelScope.launch {
                    (requestEnableNotifications as MutableLiveData).value = Event(Unit)
                }
            }
        } else if (notificationsRepository.keyProcessingOverdue) {
            ErrorState.SyncIssues
        } else {
            ErrorState.None
        }

    private fun resetAndRequestEnableNotifications() {
        viewModelScope.launch {
            notificationsRepository.requestDisableNotifications()
            (requestEnableNotifications as MutableLiveData).value = Event(Unit)
        }
    }

    fun removeExposure() {
        notificationsRepository.resetExposures()
        refreshStatus()
    }

    sealed class HeaderState(
        open val primaryAction: () -> Unit = {},
        open val secondaryAction: () -> Unit = {}
    ) {
        object Active : HeaderState()

        data class Exposed(
            val date: LocalDate,
            val clock: Clock,
            override val primaryAction: () -> Unit,
            override val secondaryAction: () -> Unit
        ) : HeaderState(primaryAction, secondaryAction)

        data class Disabled(
            override val primaryAction: () -> Unit
        ) : HeaderState(primaryAction)
    }

    sealed class ErrorState(
        open val action: () -> Unit = {}
    ) {
        object None : ErrorState()
        data class ConsentRequired(override val action: () -> Unit) : ErrorState(action)
        object SyncIssues : ErrorState()
    }
}

private fun <T> LiveData<T>.startWith(value: T): LiveData<T> {
    val mediator = MediatorLiveData<T>().apply {
        this.value = value
    }
    mediator.addSource(this) {
        mediator.value = it
    }
    return mediator
}
