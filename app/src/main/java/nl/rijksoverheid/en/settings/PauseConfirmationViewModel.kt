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
import java.time.LocalDateTime

class PauseConfirmationViewModel(private val repository: SettingsRepository) : ViewModel() {
    val skipConfirmation: LiveData<Boolean> = MutableLiveData(false)

    fun toggleDontAskForConfirmation() {
        (skipConfirmation as MutableLiveData).value = !(skipConfirmation.value ?: false)
    }

    fun setExposureNotificationsPaused(until: LocalDateTime) {
        skipConfirmation.value?.let {
            if (it) repository.setSkipPauseConfirmation(it)
        }

        repository.setExposureNotificationsPaused(until)
    }
}
