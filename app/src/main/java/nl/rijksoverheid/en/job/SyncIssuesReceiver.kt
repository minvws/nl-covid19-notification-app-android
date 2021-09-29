/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.job

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import nl.rijksoverheid.en.factory.RepositoryFactory.createExposureNotificationsRepository
import nl.rijksoverheid.en.notifier.NotificationsRepository
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SyncIssuesReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Timber.d("onReceive")
        val repository = createExposureNotificationsRepository(context)
        val async = goAsync()
        MainScope().launch {
            try {
                // prevent ANRs when this takes too long
                withTimeout(8000) {
                    if (repository.keyProcessingOverdue()) {
                        NotificationsRepository(context).showSyncIssuesNotification()
                    }
                }
            } catch (ex: TimeoutCancellationException) {
                Timber.w(ex, "Timeout while checking for overdue keys")
            } finally {
                async.finish()
            }
        }
    }

    companion object {
        fun schedule(context: Context) {
            Timber.d("Schedule")
            val alarmManager = context.getSystemService(AlarmManager::class.java)!!
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, SyncIssuesReceiver::class.java),
                PendingIntent.FLAG_CANCEL_CURRENT
            )
            alarmManager.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + TimeUnit.MINUTES.toMillis(5),
                pendingIntent
            )
        }

        fun cancel(context: Context) {
            Timber.d("Cancel notification")
            val alarmManager = context.getSystemService(AlarmManager::class.java)!!
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, SyncIssuesReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }
}
