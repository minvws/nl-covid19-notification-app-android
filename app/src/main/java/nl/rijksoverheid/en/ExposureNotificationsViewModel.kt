/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import android.app.PendingIntent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nl.rijksoverheid.en.enapi.EnableNotificationsResult
import nl.rijksoverheid.en.enapi.StatusResult
import nl.rijksoverheid.en.lifecyle.Event
import timber.log.Timber

class ExposureNotificationsViewModel(private val repository: ExposureNotificationsRepository) :
    ViewModel() {

    val notificationState: LiveData<NotificationsState> = repository.getStatus().map { result ->
        when (result) {
            is StatusResult.Enabled -> NotificationsState.Enabled
            is StatusResult.Disabled -> NotificationsState.Disabled
            is StatusResult.InvalidPreconditions -> NotificationsState.InvalidPreconditions
            is StatusResult.Unavailable -> NotificationsState.Unavailable
            is StatusResult.UnknownError -> {
                Timber.d(result.exception, "Unknown error while getting status")
                updateResult(NotificationsStatusResult.UnknownError(result.exception))
                NotificationsState.Unavailable
            }
        }
    }.asLiveData(viewModelScope.coroutineContext)

    val notificationsResult: LiveData<Event<NotificationsStatusResult>> = MutableLiveData()

    val locationPreconditionSatisfied: Boolean
        get() = repository.isLocationPreconditionSatisfied()

    fun requestEnableNotifications() {
        viewModelScope.launch {
            updateResult(repository.requestEnableNotifications())
        }
    }

    fun requestEnableNotificationsForcingConsent() {
        viewModelScope.launch {
            updateResult(repository.requestEnableNotificationsForcingConsent())
        }
    }

    private fun updateResult(result: EnableNotificationsResult) {
        when (result) {
            is EnableNotificationsResult.Enabled -> {
            }
            is EnableNotificationsResult.ResolutionRequired -> updateResult(
                NotificationsStatusResult.ConsentRequired(
                    result.resolution
                )
            )
            is EnableNotificationsResult.Unavailable -> updateResult(
                NotificationsStatusResult.Unavailable(
                    result.statusCode
                )
            )
            is EnableNotificationsResult.UnknownError -> updateResult(
                NotificationsStatusResult.UnknownError(
                    result.exception
                )
            )
        }
    }

    private fun updateResult(result: NotificationsStatusResult) {
        (notificationsResult as MutableLiveData).value = Event(result)
    }

    fun disableExposureNotifications() {
        viewModelScope.launch {
            repository.requestDisableNotifications()
        }
    }

    sealed class NotificationsState {
        object Enabled : NotificationsState()
        object InvalidPreconditions : NotificationsState()
        object Disabled : NotificationsState()
        object Unavailable : NotificationsState()
    }

    sealed class NotificationsStatusResult {
        data class ConsentRequired(val intent: PendingIntent) : NotificationsStatusResult()
        data class Unavailable(val statusCode: Int) : NotificationsStatusResult()
        data class UnknownError(val exception: Exception) : NotificationsStatusResult()
    }
}

fun LiveData<ExposureNotificationsViewModel.NotificationsState>.ignoreInitiallyEnabled(): LiveData<ExposureNotificationsViewModel.NotificationsState> =
    object : MediatorLiveData<ExposureNotificationsViewModel.NotificationsState>() {
        private var didEmitFirstValue = false

        init {
            addSource(this@ignoreInitiallyEnabled) {
                if (didEmitFirstValue || it != ExposureNotificationsViewModel.NotificationsState.Enabled) {
                    value = it
                }
                didEmitFirstValue = true
            }
        }
    }
