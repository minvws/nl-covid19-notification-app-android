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

    val requestConsent: LiveData<Event<PendingIntent>> = MutableLiveData()
    val finish: LiveData<Event<String>> = MutableLiveData()
    val unknownError: LiveData<Event<Unit>> = MutableLiveData()

    private var usedKey: String? = null

    private val refresh = MutableLiveData<Unit>()
    val keyState: LiveData<KeyState> = refresh.switchMap {
        liveData {
            emit(Loading)
            val result = labTestRepository.registerForUpload()
            if (result is RegistrationResult.Success) {
                usedKey = result.code
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
            when (val result = labTestRepository.requestUploadDiagnosticKeys()) {
                LabTestRepository.RequestUploadDiagnosisKeysResult.Success ->
                    (finish as MutableLiveData).value = Event(usedKey!!)
                LabTestRepository.RequestUploadDiagnosisKeysResult.UnknownError ->
                    (unknownError as MutableLiveData).value = Event(Unit)
                is LabTestRepository.RequestUploadDiagnosisKeysResult.RequireConsent ->
                    (requestConsent as MutableLiveData).value = Event(result.resolution)
            }
        }
    }
}
