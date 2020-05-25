/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import android.app.PendingIntent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nl.rijksoverheid.en.enapi.StartResult
import nl.rijksoverheid.en.enapi.StatusResult
import nl.rijksoverheid.en.enapi.StopResult
import nl.rijksoverheid.en.lifecyle.Event
import timber.log.Timber

class ExposureNotificationsViewModel(private val repository: ExposureNotificationsRepository) :
    ViewModel() {

    val notificationState: LiveData<NotificationsState> = MutableLiveData()
    val notificationsResult: LiveData<Event<NotificationsStatusResult>> = MutableLiveData()
    val exportTemporaryKeysResult: LiveData<Event<ExportKeysResult>> = MutableLiveData()

    val exposureDetected: LiveData<Boolean>
        get() = repository.isExposureDetected()
            .asLiveData(context = viewModelScope.coroutineContext)

    init {
        viewModelScope.launch {
            when (val result = repository.getStatus()) {
                is StatusResult.Enabled -> updateState(NotificationsState.Enabled)
                is StatusResult.Disabled -> updateState(NotificationsState.Disabled)
                is StatusResult.Unavailable -> updateState(NotificationsState.Unavailable)
                is StatusResult.UnknownError -> {
                    Timber.d(
                        result.ex,
                        "Unknown error while getting status"
                    )
                    updateResult(
                        NotificationsStatusResult.UnknownError(
                            result.ex
                        )
                    )
                }
            }
        }
    }

    fun requestEnableNotifications() {
        viewModelScope.launch {
            when (val result = repository.requestEnableNotifications()) {
                is StartResult.Started -> updateState(NotificationsState.Enabled)
                is StartResult.ResolutionRequired -> updateResult(
                    NotificationsStatusResult.ConsentRequired(
                        result.resolution
                    )
                )
                is StartResult.UnknownError -> updateResult(
                    NotificationsStatusResult.UnknownError(
                        result.ex
                    )
                )
            }
        }
    }

    fun requestDisableNotifications() {
        viewModelScope.launch {
            when (val result = repository.requestDisableNotifications()) {
                is StopResult.Stopped -> updateState(NotificationsState.Disabled)
                is StopResult.UnknownError -> updateResult(
                    NotificationsStatusResult.UnknownError(
                        result.ex
                    )
                )
            }
        }
    }

    private fun updateState(state: NotificationsState) {
        (notificationState as MutableLiveData).value = state
    }

    private fun updateResult(result: NotificationsStatusResult) {
        (notificationsResult as MutableLiveData).value = Event(result)
    }

    private fun updateResult(result: ExportKeysResult) {
        (exportTemporaryKeysResult as MutableLiveData).value = Event(result)
    }

    fun requestUploadTemporaryKeys() {
        viewModelScope.launch {
            when (val result = repository.exportTemporaryExposureKeys()) {
                is ExportTemporaryExposureKeysResult.RequireConsent -> updateResult(
                    ExportKeysResult.RequestConsent(
                        result.resolution
                    )
                )
                is ExportTemporaryExposureKeysResult.Success -> updateResult(ExportKeysResult.Success)
                is ExportTemporaryExposureKeysResult.Error -> updateResult(ExportKeysResult.Error)
            }
        }
    }

    fun resetExposures() {
        repository.resetExposures()
    }

    sealed class NotificationsState {
        object Enabled : NotificationsState()
        object Disabled : NotificationsState()
        object Unavailable : NotificationsState()
    }

    sealed class NotificationsStatusResult {
        data class ConsentRequired(val intent: PendingIntent) : NotificationsStatusResult()
        data class UnknownError(val exception: Exception) : NotificationsStatusResult()
    }

    sealed class ExportKeysResult {
        data class RequestConsent(val resolution: PendingIntent) : ExportKeysResult()
        object Success : ExportKeysResult()
        object Error : ExportKeysResult()
    }
}
