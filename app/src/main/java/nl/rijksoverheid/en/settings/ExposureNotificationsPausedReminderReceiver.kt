/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.settings

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import nl.rijksoverheid.en.notifier.NotificationsRepository
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * BroadcastReceiver which will trigger reminder notification when the app is paused.
 */
class ExposureNotificationsPausedReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("onReceive")
        NotificationsRepository(context).showAppPausedReminder()
    }

    companion object {
        fun schedule(context: Context, remindAt: LocalDateTime) {
            val pendingIntent = createPendingIntent(context)
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC,
                remindAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                pendingIntent
            )
        }

        private fun createPendingIntent(context: Context): PendingIntent {
            return PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, ExposureNotificationsPausedReminderReceiver::class.java),
                0
            )
        }

        fun cancel(context: Context) {
            val pendingIntent = createPendingIntent(context)
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
