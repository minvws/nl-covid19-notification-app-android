/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.applifecycle

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nl.rijksoverheid.en.config.AppConfigManager
import nl.rijksoverheid.en.lifecyle.Event

@SuppressLint("StaticFieldLeak")
class AppLifecycleViewModel(
    private val appLifecycleManager: AppLifecycleManager,
    private val appConfigManager: AppConfigManager
) : ViewModel() {

    val updateEvent: LiveData<Event<AppLifecycleStatus>> =
        MutableLiveData()

    fun checkForForcedAppUpdate() {
        viewModelScope.launch {
            val config = appConfigManager.getConfigOrDefault()
            if (config.deactivated) {
                (updateEvent as MutableLiveData).value = Event(AppLifecycleStatus.EndOfLife)
            } else {
                appLifecycleManager.verifyMinimumVersion(config.requiredAppVersionCode, false)
                when (val result = appLifecycleManager.getUpdateState()) {
                    is AppLifecycleManager.UpdateState.UpdateRequired,
                    is AppLifecycleManager.UpdateState.InAppUpdate -> {
                        (updateEvent as MutableLiveData).value =
                            Event(AppLifecycleStatus.Update(result))
                    }
                    else -> {
                        /* nothing, no updates */
                    }
                }
            }
        }
    }

    sealed class AppLifecycleStatus {
        data class Update(val update: AppLifecycleManager.UpdateState) :
            AppLifecycleStatus()

        object EndOfLife : AppLifecycleStatus()
    }
}
