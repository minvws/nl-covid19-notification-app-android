/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import nl.rijksoverheid.en.job.EnWorkerFactory
import nl.rijksoverheid.en.notifier.NotificationsRepository
import timber.log.Timber

@Suppress("ConstantConditionIf")
class EnApplication : Application(), Configuration.Provider {
    private val notificationsRepository = NotificationsRepository(this)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.FEATURE_LOGGING) {
            Timber.plant(Timber.DebugTree())
            Timber.plant(FileTree(getExternalFilesDir(null)))
            Timber.d("onCreate")
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
