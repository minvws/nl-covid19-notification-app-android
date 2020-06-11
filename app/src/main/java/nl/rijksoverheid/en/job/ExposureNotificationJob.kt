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
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.MainActivity
import nl.rijksoverheid.en.R

private const val KEY_TOKEN = "token"

class ExposureNotificationJob(
    context: Context,
    params: WorkerParameters,
    private val repository: ExposureNotificationsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val token = inputData.getString(KEY_TOKEN)!!
        repository.addExposure(token)
        showNotification(applicationContext)
        return Result.success()
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "exposure_notifications",
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = context.getString(R.string.notification_channel_description)
            val notificationManager: NotificationManager =
                context.getSystemService(NotificationManager::class.java)!!
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(context: Context) {
        createNotificationChannel(context)
        val intent =
            Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
        val builder =
            NotificationCompat.Builder(
                context,
                "exposure_notifications"
            )
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_message))
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.notification_message))
                )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true) // Do not reveal this notification on a secure lockscreen.
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
        val notificationManager =
            NotificationManagerCompat
                .from(context)
        notificationManager.notify(0, builder.build())
    }

    companion object {
        fun showNotification(context: Context, token: String) {
            val request = OneTimeWorkRequestBuilder<ExposureNotificationJob>()
                .setInputData(
                    Data.Builder()
                        .putString(KEY_TOKEN, token)
                        .build()
                ).build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
