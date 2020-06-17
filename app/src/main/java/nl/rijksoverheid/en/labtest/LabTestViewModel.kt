/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nl.rijksoverheid.en.labtest.LabTestViewModel.KeyState.Error
import nl.rijksoverheid.en.labtest.LabTestViewModel.KeyState.Loading
import nl.rijksoverheid.en.labtest.LabTestViewModel.KeyState.Success
import nl.rijksoverheid.en.lifecyle.Event

class LabTestViewModel(private val labTestRepository: LabTestRepository) : ViewModel() {
    sealed class KeyState {
        object Loading : KeyState()
        data class Success(val key: String) : KeyState()
        object Error : KeyState()
    }

    val uploadDiagnosisKeysResult: LiveData<Event<LabTestRepository.UploadDiagnosisKeysResult>> =
        MutableLiveData()

    private val refresh = MutableLiveData<Unit>()
    val keyState: LiveData<KeyState> = refresh.switchMap {
        liveData {
            emit(Loading)
            val result = labTestRepository.requestKey()
            if (result is RequestKeyResult.Success) {
                emit(Success(result.code))
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
            updateResult(labTestRepository.uploadDiagnosisKeys())
        }
    }

    private fun updateResult(result: LabTestRepository.UploadDiagnosisKeysResult) {
        (uploadDiagnosisKeysResult as MutableLiveData).value = Event(result)
    }
}
