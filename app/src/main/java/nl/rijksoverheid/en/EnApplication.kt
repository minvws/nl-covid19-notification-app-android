/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.work.Configuration
import androidx.work.Logger
import androidx.work.WorkManager
import androidx.work.impl.utils.ForceStopRunnable
import nl.rijksoverheid.en.job.EnWorkerFactory
import nl.rijksoverheid.en.job.PeriodicWakeUpReceiver
import nl.rijksoverheid.en.notifier.NotificationsRepository
import timber.log.Timber

@Suppress("ConstantConditionIf")
class EnApplication : Application(), Configuration.Provider {
    private val notificationsRepository = NotificationsRepository(this)

    @SuppressLint("RestrictedApi") // for WM Logger api
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.FEATURE_LOGGING) {
            Timber.plant(Timber.DebugTree())
            Timber.plant(FileTree(getExternalFilesDir(null)))
            Timber.d("onCreate")
        }
        // WM will only reschedule jobs on the job scheduler
        // when it thinks the app has been forced stopped.
        emulateWorkManagerForceStopped()

        WorkManager.initialize(this, workManagerConfiguration)
        if (BuildConfig.FEATURE_LOGGING) {
            // force init for debug logging
            Logger.setLogger(object : Logger(Log.DEBUG) {
                override fun warning(tag: String, message: String, vararg throwables: Throwable) {
                    Timber.tag(tag).w(throwables.firstOrNull(), message)
                }

                override fun info(tag: String, message: String, vararg throwables: Throwable) {
                    Timber.tag(tag).i(throwables.firstOrNull(), message)
                }

                override fun error(tag: String?, message: String?, vararg throwables: Throwable?) {
                    Timber.tag(tag).e(throwables.firstOrNull(), message)
                }

                override fun verbose(
                    tag: String?,
                    message: String?,
                    vararg throwables: Throwable?
                ) {
                    Timber.tag(tag).v(throwables.firstOrNull(), message)
                }

                override fun debug(tag: String?, message: String?, vararg throwables: Throwable?) {
                    Timber.tag(tag).d(throwables.firstOrNull(), message)
                }
            })
        }
        PeriodicWakeUpReceiver.schedule(this)
        notificationsRepository.createOrUpdateNotificationChannels()
    }

    /**
     * Make WorkManager think the app was forced stopped. As a result existing jobs
     * will be rescheduled on JobScheduler when WorkManager is initialised
     *
     * Warning: this is a workaround and depends on WorkManager internals!
     */
    private fun emulateWorkManagerForceStopped() {
        val intent = Intent(
            "ACTION_FORCE_STOP_RESCHEDULE",
            null,
            this,
            ForceStopRunnable.BroadcastReceiver::class.java
        )
        val pi = PendingIntent.getBroadcast(this, -1, intent, PendingIntent.FLAG_NO_CREATE)
        Timber.d("Pending intent = $pi")
        pi?.cancel()
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder().apply {
            setMinimumLoggingLevel(if (BuildConfig.FEATURE_LOGGING) Log.DEBUG else Log.ERROR)
            setWorkerFactory(EnWorkerFactory())
        }.build()
    }
}
