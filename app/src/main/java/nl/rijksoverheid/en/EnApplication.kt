/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.work.Configuration
import androidx.work.Logger
import androidx.work.WorkManager
import nl.rijksoverheid.en.beagle.BeagleHelperImpl
import nl.rijksoverheid.en.job.EnWorkerFactory
import nl.rijksoverheid.en.notifier.NotificationsRepository
import timber.log.Timber

@Suppress("ConstantConditionIf")
class EnApplication : Application(), Configuration.Provider {
    private val notificationsRepository by lazy { NotificationsRepository(this) }

    @SuppressLint("RestrictedApi") // for WM Logger api
    override fun onCreate() {
        super.onCreate()
        BeagleHelperImpl.initialize(this)
        if (BuildConfig.FEATURE_LOGGING) {
            Timber.plant(Timber.DebugTree())
            Timber.plant(FileTree(getExternalFilesDir(null)))
            Timber.d("onCreate")
        }

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
        notificationsRepository.createOrUpdateNotificationChannels()
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder().apply {
            setMinimumLoggingLevel(if (BuildConfig.FEATURE_LOGGING) Log.DEBUG else Log.ERROR)
            setWorkerFactory(EnWorkerFactory())
        }.build()
    }
}
