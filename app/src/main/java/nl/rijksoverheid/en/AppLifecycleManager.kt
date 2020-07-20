/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val KEY_MINIMUM_VERSION_CODE = "minimum_version_code"

class AppLifecycleManager(
    private val preferences: SharedPreferences,
    private val appUpdateManager: AppUpdateManager,
    private val onShowAppUpdateNotification: () -> Unit
) {

    /**
     * Saves the minimum version of the app so it can be checked on app open.
     * Sends a push notification if this app's version is outdated if [notify] is true
     * @param minimumVersionCode the minimum version code required
     * @param notify whether to show a notification to the user
     */
    fun verifyMinimumVersion(minimumVersionCode: Int, notify: Boolean) {
        if (minimumVersionCode != preferences.getInt(KEY_MINIMUM_VERSION_CODE, 0)) {
            preferences.edit {
                putInt(KEY_MINIMUM_VERSION_CODE, minimumVersionCode)
            }
            val currentVersionCode = BuildConfig.VERSION_CODE
            if (notify && currentVersionCode < minimumVersionCode) {
                onShowAppUpdateNotification()
            }
        }
    }

    /**
     * Checks if a forced update is necessary and if so returns the manager and info to force the update.
     */
    suspend fun getUpdateState(): UpdateState =
        suspendCoroutine { c ->
            val minimumVersionCode = preferences.getInt(KEY_MINIMUM_VERSION_CODE, 1)
            val currentVersionCode = BuildConfig.VERSION_CODE
            if (minimumVersionCode > currentVersionCode) {
                val appUpdateInfoTask = appUpdateManager.appUpdateInfo

                appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ||
                        appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                    ) {
                        c.resume(UpdateState.NeedsUpdate(appUpdateManager, appUpdateInfo))
                    } else {
                        c.resume(UpdateState.UpToDate)
                    }
                }.addOnFailureListener {
                    Timber.e("Error requesting app update state")
                    c.resume(UpdateState.Error(it))
                }
            } else {
                c.resume(UpdateState.UpToDate)
            }
        }

    sealed class UpdateState {
        data class NeedsUpdate(
            val appUpdateManager: AppUpdateManager,
            val appUpdateInfo: AppUpdateInfo
        ) : UpdateState()

        data class Error(val ex: Exception) : UpdateState()

        object UpToDate : UpdateState()
    }
}
