/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.notifier

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class NotificationsRepositoryTest {

    @Test
    fun `exposureNotificationsEnabled returns true when notifications are enabled`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val repository = NotificationsRepository(
            context
        )
        shadowOf(notificationManager).setNotificationsEnabled(true)

        assertEquals(true, repository.exposureNotificationChannelEnabled())
    }

    @Test
    fun `exposureNotificationsEnabled returns false when all notifications for app are disabled`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val repository = NotificationsRepository(
            context
        )
        shadowOf(notificationManager).setNotificationsEnabled(false)
        assertEquals(false, repository.exposureNotificationChannelEnabled())
    }

    @Test
    fun `exposureNotificationsEnabled returns false when exposure notifications channel is disabled`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val repository = NotificationsRepository(
            context
        )
        repository.createOrUpdateNotificationChannels()
        shadowOf(notificationManager).apply {
            setNotificationsEnabled(true)
            notificationChannels.map { it as NotificationChannel }
                .find { it.id == "exposure_notifications" }?.importance =
                NotificationManager.IMPORTANCE_NONE
        }

        assertEquals(false, repository.exposureNotificationChannelEnabled())
    }
}
