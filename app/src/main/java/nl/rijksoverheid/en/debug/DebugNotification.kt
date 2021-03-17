/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.debug

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.notification.ExposureNotificationReceiver

private const val ID_DEBUG_PUSH_NOTIFICATION = 3
private const val DEBUG_CHANNEL_ID = "debug_menu"

/**
 * Show a notification that allows test users to trigger a test exposure notification.
 */
class DebugNotification(private val context: Context) {
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.debug_channel_name)
            val descriptionText = context.getString(R.string.debug_channel_description)
            val importance = NotificationManager.IMPORTANCE_MIN
            val channel = NotificationChannel(DEBUG_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun show() {
        createNotificationChannel()

        val intent = Intent(context, ExposureNotificationReceiver::class.java).apply {
            action = ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED
            putExtra(
                @Suppress("DEPRECATION")
                ExposureNotificationClient.EXTRA_TOKEN,
                ExposureNotificationsRepository.DEBUG_TOKEN
            )
        }
        val pendingIntent: PendingIntent =
            PendingIntent.getBroadcast(context, 0, intent, 0)

        val builder = NotificationCompat.Builder(context, DEBUG_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.debug_notification_title))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .addAction(
                R.drawable.ic_notification,
                context.getString(R.string.debug_notification_trigger_exposure_notification),
                pendingIntent
            )

        NotificationManagerCompat.from(context).notify(ID_DEBUG_PUSH_NOTIFICATION, builder.build())
    }
}
