/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.settings

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import nl.rijksoverheid.en.notifier.NotificationsRepository
import java.time.LocalDateTime

/**
 * Repository class for in app settings stored locally.
 */
class SettingsRepository(private val context: Context, private val settings: Settings) {

    var wifiOnly: Boolean
        get() = settings.checkOnWifiOnly
        set(value) { settings.checkOnWifiOnly = value }

    var skipPauseConfirmation: Boolean
        get() = settings.skipPauseConfirmation
        set(value) { settings.skipPauseConfirmation = value }

    var dashboardEnabled: Boolean
        get() = settings.dashboardEnabled
        set(value) { settings.dashboardEnabled = value }

    fun exposureNotificationsPausedState(): Flow<Settings.PausedState> =
        settings.observeChanges().map { it.exposureStatePausedState }.distinctUntilChanged()

    fun isPaused() =
        settings.exposureStatePausedState is Settings.PausedState.Paused

    fun setExposureNotificationsPaused(until: LocalDateTime) {
        ExposureNotificationsPausedReminderReceiver.schedule(context, until)
        settings.setExposureNotificationsPaused(until)
    }

    fun rescheduleReminder() {
        val pausedState = settings.exposureStatePausedState
        if (pausedState is Settings.PausedState.Paused) {
            setExposureNotificationsPaused(pausedState.pausedUntil)
        }
    }

    fun clearExposureNotificationsPaused() {
        ExposureNotificationsPausedReminderReceiver.cancel(context)
        NotificationsRepository(context).clearAppPausedNotification()
        settings.clearExposureNotificationsPaused()
    }

    fun getDashboardEnabledFlow(): Flow<Boolean> =
        settings.observeChanges().map { it.dashboardEnabled }.distinctUntilChanged()
}
