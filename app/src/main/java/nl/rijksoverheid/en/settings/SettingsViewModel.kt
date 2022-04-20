/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.map
import nl.rijksoverheid.en.lifecyle.Event
import java.time.LocalDateTime

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val pausedState: LiveData<Settings.PausedState.Paused?> = repository.exposureNotificationsPausedState().map {
        when (it) {
            Settings.PausedState.Enabled -> null
            is Settings.PausedState.Paused -> it
        }
    }.asLiveData(viewModelScope.coroutineContext)

    val wifiOnly = MutableLiveData(repository.wifiOnly)
    val wifiOnlyChanged: LiveData<Event<Boolean>> = MutableLiveData()
    val pauseRequested: LiveData<Event<Unit>> = MutableLiveData()
    val enableExposureNotificationsRequested: LiveData<Event<Unit>> = MutableLiveData()
    val dashboardEnabled = MutableLiveData(repository.dashboardEnabled)

    val skipPauseConfirmation: Boolean
        get() = repository.skipPauseConfirmation

    fun wifiOnlyChanged(checked: Boolean) {
        repository.wifiOnly = checked
        (wifiOnlyChanged as MutableLiveData).value = Event(checked)
    }

    fun requestPauseExposureNotifications() {
        (pauseRequested as MutableLiveData).value = Event(Unit)
    }

    fun setExposureNotificationsPaused(until: LocalDateTime) {
        repository.setExposureNotificationsPaused(until)
    }

    fun enableExposureNotifications() {
        (enableExposureNotificationsRequested as MutableLiveData).value = Event(Unit)
    }

    fun dashboardEnabledChanged(enabled: Boolean) {
        repository.dashboardEnabled = enabled
    }
}
