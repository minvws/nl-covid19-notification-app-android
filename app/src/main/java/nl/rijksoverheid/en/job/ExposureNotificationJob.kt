/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.job

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDeepLinkBuilder
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.R
import java.time.Clock
import java.time.LocalDate

private const val KEY_TOKEN = "token"

class ExposureNotificationJob(
    context: Context,
    params: WorkerParameters,
    private val repository: ExposureNotificationsRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val token = inputData.getString(KEY_TOKEN)!!
        val daysSinceLastExposure = repository.addExposure(token)
        // TODO Question: Should we always trigger a notification, or only if the exposure is newer?
        // Only show notification when this exposure is the most recent and still valid
        if (daysSinceLastExposure != null) {
            showNotification(applicationContext, daysSinceLastExposure)
        }
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

    private fun showNotification(context: Context, daysSinceLastExposure: Int) {
        createNotificationChannel(context)
        val dayOfLastExposure = LocalDate.now(clock)
            .minusDays(daysSinceLastExposure.toLong()).toEpochDay()

        val pendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_main)
            .setDestination(R.id.nav_post_notification)
            .setArguments(Bundle().apply { putLong("epochDayOfLastExposure", dayOfLastExposure) })
            .createPendingIntent()
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
