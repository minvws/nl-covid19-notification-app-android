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

    val uploadResult: LiveData<Event<UploadResult>> = MutableLiveData()

    var usedKey: String? = null

    private val refresh = MutableLiveData<Unit>()
    val keyState: LiveData<KeyState> = refresh.switchMap {
        liveData {
            emit(Loading)
            val result = labTestRepository.registerForUpload()
            if (result is RegistrationResult.Success) {
                val state = Success(result.code)
                usedKey = state.displayKey
                emit(state)
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
            updateResult(
                when (val result = labTestRepository.requestUploadDiagnosticKeys()) {
                    LabTestRepository.RequestUploadDiagnosisKeysResult.Success ->
                        UploadResult.Success(usedKey!!)
                    LabTestRepository.RequestUploadDiagnosisKeysResult.UnknownError ->
                        UploadResult.Error
                    is LabTestRepository.RequestUploadDiagnosisKeysResult.RequireConsent ->
                        UploadResult.RequestConsent(result.resolution)
                }
            )
        }
    }

    private fun updateResult(result: UploadResult) {
        (uploadResult as MutableLiveData).value = Event(result)
    }

    sealed class UploadResult {
        data class RequestConsent(val resolution: PendingIntent) : UploadResult()
        data class Success(val usedKey: String) : UploadResult()
        object Error : UploadResult()
    }

    sealed class KeyState {
        object Loading : KeyState()
        data class Success(private val code: String) : KeyState() {
            val displayKey: String
            val key: String
            init {
                if (code.contains('-')) {
                    // Code is 6-character displayKey with dashes (cached from registration v1)
                    displayKey = code
                    key = code.replace("-", "")
                } else {
                    // Code is 7-character key without dashes (from registration v2)
                    val keyPart1 = code.substring(0..2)
                    val keyPart2 = code.substring(3..4)
                    val keyPart3 = code.substring(5..6)
                    displayKey = "$keyPart1-$keyPart2-$keyPart3"
                    key = code
                }
            }
        }

        object Error : KeyState()
    }
}
