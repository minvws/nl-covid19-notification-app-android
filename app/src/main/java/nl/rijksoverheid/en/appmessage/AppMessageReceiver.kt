/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.appmessage

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import nl.rijksoverheid.en.api.model.AppMessage
import nl.rijksoverheid.en.factory.RepositoryFactory.createAppConfigManager
import nl.rijksoverheid.en.factory.RepositoryFactory.createResourceBundleManager
import nl.rijksoverheid.en.notifier.NotificationsRepository
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * BroadcastReceiver which will trigger notification based on AppConfig.notification.
 */
class AppMessageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("onReceive")

        val appConfigManager = createAppConfigManager(context)
        val resourceBundleManager = createResourceBundleManager(context)

        val async = goAsync()
        MainScope().launch {
            val appMessage = appConfigManager.getCachedConfigOrDefault().notification ?: return@launch
            val (title, message) = resourceBundleManager.getAppMessageResources(
                appMessage.title,
                appMessage.body
            )
            val destination = AppMessage.TargetScreenOption.values().firstOrNull {
                it.value == appMessage.targetScreen
            }

            NotificationsRepository(context).showAppMessageNotification(title, message, destination)
            async.finish()
        }
    }

    companion object {

        /**
         * Schedules or updates an scheduled [AppMessageReceiver]
         */
        fun schedule(context: Context, scheduledDateTime: LocalDateTime) {
            if (scheduledDateTime.isBefore(LocalDateTime.now())) {
                Timber.d("ScheduledDateTime has already passed")
                return
            }

            Timber.d("Schedule")
            val pendingIntent = createPendingIntent(context)
            val alarmManager = context.getSystemService(AlarmManager::class.java)

            alarmManager.set(
                AlarmManager.RTC,
                scheduledDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                pendingIntent
            )
        }

        private fun createPendingIntent(context: Context): PendingIntent {
            return PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, AppMessageReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
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
