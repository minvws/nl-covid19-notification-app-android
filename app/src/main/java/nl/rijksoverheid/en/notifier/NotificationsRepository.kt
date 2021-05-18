/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.notifier

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.NavDeepLinkBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.job.RemindExposureNotificationWorker
import nl.rijksoverheid.en.lifecyle.asFlow
import nl.rijksoverheid.en.util.formatDaysSince
import java.time.Clock
import java.time.LocalDate

private const val EXPOSURE_NOTIFICATION_CHANNEL_ID = "exposure_notifications"
private const val EXPOSURE_NOTIFICATION_ID = 1
private const val SYNC_ISSUES_NOTIFICATION_CHANNEL_ID = "sync_issue_notifications"
private const val SYNC_ISSUES_NOTIFICATION_ID = 2
private const val APP_UPDATE_NOTIFICATION_CHANNEL_ID = "update_notifications"
private const val APP_UPDATE_NOTIFICATION_ID = 3
private const val UPLOAD_KEYS_FAILED_ID = 4
private const val APP_INACTIVE_NOTIFICATION_ID = 5
private const val REMINDER_NOTIFICATION_CHANNEL_ID = "reminders"
private const val PAUSED_REMINDER_NOTIFICATION_ID = 6

class NotificationsRepository(
    private val context: Context,
    private val clock: Clock = Clock.systemDefaultZone(),
    lifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get()
) {

    private val refreshOnStart = lifecycleOwner.asFlow().filter { it == Lifecycle.State.STARTED }
        .map { Unit }.onStart { emit(Unit) }

    fun exposureNotificationsEnabled(): Flow<Boolean> = refreshOnStart.map {
        val allNotificationsEnabled =
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        val exposureNotificationsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManagerCompat.from(context)
                .getNotificationChannel(EXPOSURE_NOTIFICATION_CHANNEL_ID)?.importance !=
                NotificationManager.IMPORTANCE_NONE
        } else {
            true
        }

        allNotificationsEnabled && exposureNotificationsEnabled
    }

    fun createOrUpdateNotificationChannels() {
        createNotificationChannel(
            EXPOSURE_NOTIFICATION_CHANNEL_ID,
            R.string.notification_channel_name,
            R.string.notification_channel_description,
            true,
            NotificationCompat.VISIBILITY_SECRET
        )
        createNotificationChannel(
            SYNC_ISSUES_NOTIFICATION_CHANNEL_ID,
            R.string.sync_issue_channel_name,
            R.string.sync_issue_channel_description
        )
        createNotificationChannel(
            APP_UPDATE_NOTIFICATION_CHANNEL_ID,
            R.string.update_channel_name,
            R.string.update_channel_description
        )
        createNotificationChannel(
            REMINDER_NOTIFICATION_CHANNEL_ID,
            R.string.reminder_channel_name,
            R.string.reminder_channel_description
        )
    }

    fun showSyncIssuesNotification() {
        showNotification(
            SYNC_ISSUES_NOTIFICATION_ID,
            createNotification(
                SYNC_ISSUES_NOTIFICATION_CHANNEL_ID,
                R.string.sync_issue_notification_title,
                R.string.sync_issue_notification_message
            ).build()
        )
    }

    fun cancelSyncIssuesNotification() {
        NotificationManagerCompat.from(context).cancel(SYNC_ISSUES_NOTIFICATION_ID)
    }

    fun showExposureNotification(lastExposureDate: LocalDate, notificationReceivedDate: LocalDate?, reminder: Boolean = false) {
        val pendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_main)
            .setDestination(R.id.nav_post_notification)
            .setArguments(
                Bundle().apply {
                    putSerializable("lastExposureLocalDateString", lastExposureDate.toString())
                    putSerializable("notificationReceivedLocalDateString", notificationReceivedDate?.toString())
                }
            ).createPendingIntent()
        val message = context.getString(
            R.string.notification_message,
            lastExposureDate.formatDaysSince(context, clock)
        )
        val builder = createNotification(
            EXPOSURE_NOTIFICATION_CHANNEL_ID,
            R.string.notification_title,
            if (reminder) context.getString(
                R.string.notification_message_reminder_prefix,
                message
            ) else message
        ).setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setContentIntent(pendingIntent).apply {
                if (reminder) {
                    addAction(
                        NotificationCompat.Action.Builder(
                            R.drawable.ic_close,
                            context.getString(R.string.notification_message_reminder_dismiss),
                            DismissReminderReceiver.getPendingIntent(context)
                        ).build()
                    )
                }
            }

        showNotification(EXPOSURE_NOTIFICATION_ID, builder.build())
    }

    fun cancelExposureNotification() {
        NotificationManagerCompat.from(context).cancel(EXPOSURE_NOTIFICATION_ID)
        RemindExposureNotificationWorker.cancel(context)
    }

    fun showAppUpdateNotification() {
        showNotification(
            APP_UPDATE_NOTIFICATION_ID,
            createNotification(
                APP_UPDATE_NOTIFICATION_CHANNEL_ID,
                R.string.update_notification_title,
                R.string.update_notification_message
            ).build()
        )
    }

    fun showUploadKeysFailedNotification() {
        val pendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_main)
            .setDestination(R.id.nav_upload_keys_failed_notification)
            .createPendingIntent()

        showNotification(
            UPLOAD_KEYS_FAILED_ID,
            createNotification(
                SYNC_ISSUES_NOTIFICATION_CHANNEL_ID,
                R.string.upload_keys_failed_title,
                R.string.upload_keys_failed_message
            ).setContentIntent(pendingIntent).build()
        )
    }

    fun showAppInactiveNotification() {
        showNotification(
            APP_INACTIVE_NOTIFICATION_ID,
            createNotification(
                SYNC_ISSUES_NOTIFICATION_CHANNEL_ID,
                R.string.app_inactive_title,
                R.string.app_inactive_message
            ).setPriority(NotificationCompat.PRIORITY_DEFAULT).setOngoing(true).build()
        )
    }

    fun clearAppInactiveNotification() {
        NotificationManagerCompat.from(context).cancel(APP_INACTIVE_NOTIFICATION_ID)
    }

    fun showAppPausedReminder() {
        showNotification(
            PAUSED_REMINDER_NOTIFICATION_ID,
            createNotification(
                REMINDER_NOTIFICATION_CHANNEL_ID,
                R.string.app_inactive_paused_title,
                R.string.app_inactive_paused_message,
                false
            ).setPriority(NotificationCompat.PRIORITY_DEFAULT).setOngoing(true).build()
        )
    }

    fun clearAppPausedNotification() {
        NotificationManagerCompat.from(context).cancel(PAUSED_REMINDER_NOTIFICATION_ID)
    }

    private fun showNotification(id: Int, notification: Notification) {
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun createNotificationChannel(
        id: String,
        @StringRes name: Int,
        @StringRes description: Int,
        enableVibration: Boolean = false,
        lockscreenVisibility: Int = NotificationCompat.VISIBILITY_PUBLIC
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                id,
                context.getString(name),
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = context.getString(description)
            channel.enableVibration(enableVibration)
            channel.lockscreenVisibility = lockscreenVisibility
            val notificationManager: NotificationManager =
                context.getSystemService(NotificationManager::class.java)!!
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(
        channelId: String,
        @StringRes title: Int,
        @StringRes message: Int,
        autoCancel: Boolean = true
    ) = createNotification(channelId, title, context.getString(message), autoCancel)

    private fun createNotification(
        channel: String,
        @StringRes title: Int,
        message: String,
        autoCancel: Boolean = true
    ): NotificationCompat.Builder {
        val pendingIntent = NavDeepLinkBuilder(context).setGraph(R.navigation.nav_main)
            .setDestination(R.id.main_nav).createPendingIntent()
        return NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(title))
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setAutoCancel(autoCancel)
    }
}
