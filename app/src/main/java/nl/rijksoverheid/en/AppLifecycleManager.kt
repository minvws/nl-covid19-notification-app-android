/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val KEY_MINIMUM_VERSION_CODE = "minimum_version_code"

open class AppLifecycleManager(
    private val context: Context,
    private val preferences: SharedPreferences,
    private val appUpdateManager: AppUpdateManager
) {

    /**
     * Saves the minimum version of the app so it can be checked on app open.
     * Sends a push notification if this app's version is outdated.
     */
    fun verifyMinimumVersion(minimumVersionCode: Int) {
        preferences.edit {
            putInt(KEY_MINIMUM_VERSION_CODE, minimumVersionCode)
        }
        val currentVersionCode = BuildConfig.VERSION_CODE
        if (currentVersionCode < minimumVersionCode) {
            showNotification()
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "update_notifications",
                context.getString(R.string.update_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = context.getString(R.string.update_channel_description)
            val notificationManager: NotificationManager =
                context.getSystemService(NotificationManager::class.java)!!
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java)
        intent.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
        val notification =
            NotificationCompat.Builder(context, "update_notifications")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.update_notification_title))
                .setContentText(context.getString(R.string.update_notification_message))
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.update_notification_message))
                )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .build()

        NotificationManagerCompat.from(context).notify(0, notification)
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
