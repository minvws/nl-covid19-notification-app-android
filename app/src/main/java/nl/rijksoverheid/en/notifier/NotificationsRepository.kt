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
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDeepLinkBuilder
import nl.rijksoverheid.en.MainActivity
import nl.rijksoverheid.en.R
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

class NotificationsRepository(
    private val context: Context,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    fun createOrUpdateNotificationChannels() {
        createNotificationChannel(
            EXPOSURE_NOTIFICATION_CHANNEL_ID,
            R.string.notification_channel_name,
            R.string.notification_channel_description
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

    fun showExposureNotification(daysSinceLastExposure: Int) {

        val dateOfLastExposure = LocalDate.now(clock)
            .minusDays(daysSinceLastExposure.toLong())

        val pendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_main)
            .setDestination(R.id.nav_post_notification)
            .setArguments(Bundle().apply {
                putLong("epochDayOfLastExposure", dateOfLastExposure.toEpochDay())
            }).createPendingIntent()
        val message = context.getString(
            R.string.notification_message,
            dateOfLastExposure.formatDaysSince(context, clock)
        )
        val builder = createNotification(
            EXPOSURE_NOTIFICATION_CHANNEL_ID,
            R.string.notification_title,
            message
        )
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setContentIntent(pendingIntent)

        showNotification(EXPOSURE_NOTIFICATION_ID, builder.build())
    }

    fun cancelExposureNotification() {
        NotificationManagerCompat.from(context).cancel(EXPOSURE_NOTIFICATION_ID)
    }

    fun showAppUpdateNotification() {
        showNotification(
            APP_UPDATE_NOTIFICATION_ID, createNotification(
                APP_UPDATE_NOTIFICATION_CHANNEL_ID,
                R.string.update_notification_title,
                R.string.update_notification_message
            ).build()
        )
    }

    fun showUploadKeysFailedNotification() {
        //TODO deeplink to the correct screen
        showNotification(
            UPLOAD_KEYS_FAILED_ID, createNotification(
                SYNC_ISSUES_NOTIFICATION_CHANNEL_ID,
                R.string.upload_keys_failed_title,
                R.string.upload_keys_failed_message
            ).build()
        )
    }

    private fun showNotification(id: Int, notification: Notification) {
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun createNotificationChannel(
        id: String,
        @StringRes name: Int,
        @StringRes description: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                id,
                context.getString(name),
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = context.getString(description)
            val notificationManager: NotificationManager =
                context.getSystemService(NotificationManager::class.java)!!
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(
        channelId: String,
        @StringRes title: Int,
        @StringRes message: Int
    ) = createNotification(channelId, title, context.getString(message))

    private fun createNotification(
        channel: String,
        @StringRes title: Int,
        message: String
    ): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
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
            .setAutoCancel(true)
    }
}
