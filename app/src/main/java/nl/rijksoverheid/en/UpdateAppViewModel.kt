/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nl.rijksoverheid.en.lifecyle.Event

class UpdateAppViewModel(private val appLifecycleManager: AppLifecycleManager) : ViewModel() {

    val updateEvent: LiveData<Event<AppLifecycleManager.UpdateState.NeedsUpdate>> =
        MutableLiveData()

    init {
        checkForForcedAppUpdate()
    }

    fun verifyDownloadNotStalled() {
        checkForForcedAppUpdate()
    }

    private fun checkForForcedAppUpdate() {
        viewModelScope.launch {
            when (val result = appLifecycleManager.getUpdateState()) {
                is AppLifecycleManager.UpdateState.NeedsUpdate -> {
                    (updateEvent as MutableLiveData).value = Event(result)
                }
                else -> {
                } // ignore
            }
        }
    }
}
