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
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import nl.rijksoverheid.en.lifecyle.Event
import java.time.LocalDateTime

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val pausedState: LiveData<Settings.PausedState> = repository.exposureNotificationsPausedState()
        .asLiveData(viewModelScope.coroutineContext)

    val exposureNotificationsPaused: LiveData<Boolean> =
        pausedState.map { it is Settings.PausedState.Paused }

    // updated from the view
    val wifiOnly = MutableLiveData(repository.wifiOnly)
    val wifiOnlyChanged: LiveData<Event<Boolean>> = MutableLiveData()
    val pauseRequested: LiveData<Event<Unit>> = MutableLiveData()
    val enableExposureNotificationsRequested: LiveData<Event<Unit>> = MutableLiveData()

    val skipPauseConfirmation: Boolean
        get() = repository.skipPauseConfirmation

    init {
        var firstValue = true
        wifiOnly.observeForever {
            if (!firstValue) {
                repository.setWifiOnly(it)
                (wifiOnlyChanged as MutableLiveData).value = Event(it)
            }
            firstValue = false
        }
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
}
