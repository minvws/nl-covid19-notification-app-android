/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.applifecycle

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import nl.rijksoverheid.en.BuildConfig

private const val KEY_MINIMUM_VERSION_CODE = "minimum_version_code"
private const val PLAY_STORE_PACKAGE_NAME = "com.android.vending"

class AppLifecycleManager(
    private val context: Context,
    private val preferences: SharedPreferences,
    private val appUpdateManager: AppUpdateManager,
    private val currentVersionCode: Int = BuildConfig.VERSION_CODE,
    private val onShowAppUpdateNotification: () -> Unit
) {

    private val minimumVersionCode: Int
        get() = preferences.getInt(KEY_MINIMUM_VERSION_CODE, 0)

    /**
     * Saves the minimum version of the app so it can be checked on app open.
     * Sends a push notification if this app's version is outdated if [notify] is true
     * @param minimumVersionCode the minimum version code required
     * @param notify whether to show a notification to the user
     */
    fun verifyMinimumVersion(minimumVersionCode: Int, notify: Boolean) {
        if (minimumVersionCode != this.minimumVersionCode) {
            preferences.edit {
                putInt(KEY_MINIMUM_VERSION_CODE, minimumVersionCode)
            }
            if (notify && currentVersionCode < minimumVersionCode) {
                onShowAppUpdateNotification()
            }
        }
    }

    /**
     * Checks if a forced update is necessary and if so returns the manager and info to force the update.
     */
    suspend fun getUpdateState(): UpdateState {
        return if (minimumVersionCode > currentVersionCode) {
            when (val source = getInstallPackageName()) {
                PLAY_STORE_PACKAGE_NAME -> {
                    val appUpdateInfo = appUpdateManager.requestAppUpdateInfo()
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ||
                        appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                    ) {
                        UpdateState.InAppUpdate(appUpdateManager, appUpdateInfo)
                    } else {
                        // update might not be available for in-app update, for example for a staged roll out
                        UpdateState.UpToDate
                    }
                }
                else -> UpdateState.UpdateRequired(source)
            }
        } else {
            UpdateState.UpToDate
        }
    }

    sealed class UpdateState {
        data class InAppUpdate(
            val appUpdateManager: AppUpdateManager,
            val appUpdateInfo: AppUpdateInfo
        ) : UpdateState()

        data class UpdateRequired(val installerPackageName: String?) : UpdateState()

        data class Error(val ex: Exception) : UpdateState()

        object UpToDate : UpdateState()
    }

    private fun getInstallPackageName(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName)
        }
    }
}
