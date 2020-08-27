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
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Receiver that is periodically pinged on best-effort basis using an alarm. As a result
 * it will init the application if the app process wasn't around and it will nudge
 * WorkManagers in-process scheduler to kick off jobs if needed
 */
class PeriodicWakeUpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("onReceive")
        // this will nudge the in process scheduler
        TriggerWorker.schedule(context)
    }

    companion object {
        fun schedule(context: Context) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val operation = PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, PeriodicWakeUpReceiver::class.java),
                PendingIntent.FLAG_CANCEL_CURRENT
            )
            Timber.d("Schedule periodic wake up")
            alarmManager?.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + TimeUnit.HOURS.toMillis(1L),
                TimeUnit.HOURS.toMillis(1L),
                operation
            )
        }
    }
}
