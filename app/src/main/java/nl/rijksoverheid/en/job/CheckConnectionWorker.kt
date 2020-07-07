/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.job

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.MainActivity
import nl.rijksoverheid.en.R
import timber.log.Timber
import java.util.concurrent.TimeUnit

private const val WORKER_ID = "check_connection"
private const val ID_CONNECTION_PUSH_NOTIFICATION = 2

class CheckConnectionWorker(
    private val context: Context,
    params: WorkerParameters,
    private val repository: ExposureNotificationsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Timber.d("Check if key processing is overdue")
        if (repository.keyProcessingOverdue) {
            Timber.d("Key processing is overdue")
            showNotification()
        }
        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "sync_issue_notifications",
                context.getString(R.string.sync_issue_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = context.getString(R.string.sync_issue_channel_description)
            val notificationManager: NotificationManager =
                context.getSystemService(NotificationManager::class.java)!!
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
        val notification =
            NotificationCompat.Builder(context, "sync_issue_notifications")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.sync_issue_notification_title))
                .setContentText(context.getString(R.string.sync_issue_notification_message))
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.sync_issue_notification_message))
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .build()

        NotificationManagerCompat.from(context)
            .notify(ID_CONNECTION_PUSH_NOTIFICATION, notification)
    }

    companion object {
        fun queue(context: Context) {
            val request = PeriodicWorkRequestBuilder<CheckConnectionWorker>(1, TimeUnit.DAYS)
                .apply {
                    if (BuildConfig.DEBUG) {
                        setInitialDelay(10, TimeUnit.SECONDS)
                    } else {
                        setInitialDelay(1, TimeUnit.DAYS)
                    }
                }.build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORKER_ID,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    request
                )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORKER_ID)
        }
    }
}
