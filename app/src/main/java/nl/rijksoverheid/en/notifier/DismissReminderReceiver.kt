/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.notifier

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import nl.rijksoverheid.en.job.RemindExposureNotificationWorker

/**
 * BroadcastReceiver for dismissing reminder notifications when exposed
 */
class DismissReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationsRepository(context).cancelExposureNotification()
        RemindExposureNotificationWorker.cancel(context)
    }

    companion object {
        fun getPendingIntent(context: Context): PendingIntent {
            return PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, DismissReminderReceiver::class.java),
                PendingIntent.FLAG_CANCEL_CURRENT
            )
        }
    }
}
