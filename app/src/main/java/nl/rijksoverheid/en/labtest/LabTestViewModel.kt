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
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nl.rijksoverheid.en.labtest.LabTestViewModel.KeyState.Loading

class LabTestViewModel(private val labTestRepository: LabTestRepository) : ViewModel() {
    sealed class KeyState {
        object Loading : KeyState()
        data class Success(val key: String) : KeyState()
        object Error : KeyState()
    }

    val keyState: LiveData<KeyState> = MutableLiveData(Loading)

    init {
        retrieveKey()
    }

    fun retry() {
        (keyState as MutableLiveData).value = Loading
        retrieveKey()
    }

    private fun retrieveKey() {
        viewModelScope.launch {
            val keyResult = labTestRepository.requestKey()
            (keyState as MutableLiveData).value = if (keyResult is RequestKeyResult.Success) {
                KeyState.Success(keyResult.key)
            } else {
                KeyState.Error
            }
        }
    }
}
