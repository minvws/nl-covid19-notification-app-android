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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import nl.rijksoverheid.en.config.AppConfigManager
import timber.log.Timber

private const val NAVIGATION_TRANSITION_DURATION = 300L

/**
 * ViewModel containing logic regarding app lifecycles like required updates or deactivation.
 */
class AppLifecycleViewModel(
    private val appLifecycleManager: AppLifecycleManager,
    private val appConfigManager: AppConfigManager
) : ViewModel() {

    private var checkForForcedAppUpdateJob: Job? = null

    private val _updateEvent: MutableStateFlow<AppLifecycleStatus?> = MutableStateFlow(null)
    val updateEvent: LiveData<AppLifecycleStatus> = _updateEvent
        .filterNotNull()
        .asLiveData(viewModelScope.coroutineContext)

    private var initialCheckInProgress: Boolean = true

    val splashScreenKeepOnScreenCondition: Boolean get() {
        return initialCheckInProgress
    }

    init {
        checkForForcedAppUpdate()
    }

    /**
     * Check in app config for required [AppLifecycleStatus].
     */
    fun checkForForcedAppUpdate() {
        if (checkForForcedAppUpdateJob?.isActive == true)
            return

        checkForForcedAppUpdateJob = viewModelScope.launch {
            try {
                val config = appConfigManager.getConfig()
                if (config.deactivated) {
                    _updateEvent.emit(AppLifecycleStatus.EndOfLife)
                } else {
                    appLifecycleManager.verifyMinimumVersion(config.requiredAppVersionCode, false)
                    when (val result = appLifecycleManager.getUpdateState()) {
                        is AppLifecycleManager.UpdateState.UpdateRequired,
                        is AppLifecycleManager.UpdateState.InAppUpdate -> {
                            _updateEvent.emit(AppLifecycleStatus.Update(result))
                        }
                        else -> {
                            _updateEvent.emit(AppLifecycleStatus.Ready)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Error getting app config")
                _updateEvent.emit(AppLifecycleStatus.UnableToFetchAppConfig)
            } finally {
                if (_updateEvent.value !is AppLifecycleStatus.Ready)
                    delay(NAVIGATION_TRANSITION_DURATION)
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
