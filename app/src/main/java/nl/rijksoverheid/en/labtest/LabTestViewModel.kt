/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest

import android.app.PendingIntent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nl.rijksoverheid.en.labtest.LabTestRepository.ScheduleUploadTeksResult
import nl.rijksoverheid.en.labtest.LabTestViewModel.KeyState.Error
import nl.rijksoverheid.en.labtest.LabTestViewModel.KeyState.Loading
import nl.rijksoverheid.en.labtest.LabTestViewModel.KeyState.Success
import nl.rijksoverheid.en.labtest.LabTestViewModel.UploadKeysResult.RequireConsent
import nl.rijksoverheid.en.lifecyle.Event

class LabTestViewModel(private val labTestRepository: LabTestRepository) : ViewModel() {
    sealed class KeyState {
        object Loading : KeyState()
        data class Success(val key: String) : KeyState()
        object Error : KeyState()
    }

    val uploadKeysResult: LiveData<Event<UploadKeysResult>> = MutableLiveData()

    private val refresh = MutableLiveData(Unit)
    val keyState: LiveData<KeyState> = refresh.switchMap {
        liveData {
            emit(Loading)
            val result = labTestRepository.requestKey()
            if (result is RequestKeyResult.Success) {
                emit(Success(result.key))
            } else {
                emit(Error)
            }
        }
    }

    fun retry() {
        refresh.value = Unit
    }

    fun upload() {
        viewModelScope.launch {
            when (val result = labTestRepository.scheduleUploadTeks()) {
                is ScheduleUploadTeksResult.RequireConsent -> updateResult(RequireConsent(result.resolution))
                ScheduleUploadTeksResult.Success -> updateResult(UploadKeysResult.Success)
            }
        }
    }

    private fun updateResult(result: UploadKeysResult) {
        (uploadKeysResult as MutableLiveData).value = Event(result)
    }

    sealed class UploadKeysResult {
        data class RequireConsent(val resolution: PendingIntent) : UploadKeysResult()
        object Success : UploadKeysResult()
    }
}
