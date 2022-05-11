/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.applifecycle

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import nl.rijksoverheid.en.config.AppConfigManager
import timber.log.Timber

/**
 * ViewModel containing logic regarding app lifecycles like required updates or deactivation.
 */
class AppLifecycleViewModel(
    private val appLifecycleManager: AppLifecycleManager,
    private val appConfigManager: AppConfigManager
) : ViewModel() {

    private var checkForForcedAppUpdateJob: Job? = null

    private val _appLifecycleStatus: MutableStateFlow<AppLifecycleStatus?> = MutableStateFlow(null)
    val appLifecycleStatus: LiveData<AppLifecycleStatus> = _appLifecycleStatus
        .filterNotNull()
        .asLiveData(viewModelScope.coroutineContext)

    private var initialCheckInProgress: Boolean = true

    val splashScreenKeepOnScreenCondition: Boolean get() {
        return initialCheckInProgress
    }

    init {
        checkAppLifecycleStatus()
    }

    /**
     * Check in app config for required [AppLifecycleStatus].
     */
    fun checkAppLifecycleStatus() {
        if (checkForForcedAppUpdateJob?.isActive == true)
            return

        checkForForcedAppUpdateJob = viewModelScope.launch {
            try {
                val config = appConfigManager.getConfig()
                if (config.deactivated) {
                    _appLifecycleStatus.emit(AppLifecycleStatus.EndOfLife)
                } else {
                    appLifecycleManager.verifyMinimumVersion(config.requiredAppVersionCode, false)
                    when (val result = appLifecycleManager.getUpdateState()) {
                        is AppLifecycleManager.UpdateState.UpdateRequired,
                        is AppLifecycleManager.UpdateState.InAppUpdate -> {
                            _appLifecycleStatus.emit(AppLifecycleStatus.Update(result))
                        }
                        else -> {
                            _appLifecycleStatus.emit(AppLifecycleStatus.Ready)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Error getting app config")
                _appLifecycleStatus.emit(AppLifecycleStatus.UnableToFetchAppConfig)
            } finally {
                initialCheckInProgress = false
            }
        }
    }

    sealed class AppLifecycleStatus {
        data class Update(val update: AppLifecycleManager.UpdateState) :
            AppLifecycleStatus()

        object EndOfLife : AppLifecycleStatus()
        object UnableToFetchAppConfig : AppLifecycleStatus()
        object Ready : AppLifecycleStatus()
    }
}
