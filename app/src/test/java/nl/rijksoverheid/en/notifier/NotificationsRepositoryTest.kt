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
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.O_MR1])
class NotificationsRepositoryTest {

    @Test
    fun `exposureNotificationsEnabled returns true when notifications are enabled`() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Application>()
            val lifecycleOwner = ProcessLifecycleOwner.get()
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            val repository = NotificationsRepository(
                context,
                lifecycleOwner = lifecycleOwner
            )
            shadowOf(notificationManager).setNotificationsEnabled(true)

            assertEquals(true, repository.exposureNotificationsEnabled().first())
        }

    @Test
    fun `exposureNotificationsEnabled returns false when all notifications for app are disabled`() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Application>()
            val lifecycleOwner = ProcessLifecycleOwner.get()
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            val repository = NotificationsRepository(
                context,
                lifecycleOwner = lifecycleOwner
            )
            shadowOf(notificationManager).setNotificationsEnabled(false)
            assertEquals(false, repository.exposureNotificationsEnabled().first())
        }

    @Test
    fun `exposureNotificationsEnabled returns false when exposure notifications channel is disabled`() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Application>()
            val lifecycleOwner = ProcessLifecycleOwner.get()
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            val repository = NotificationsRepository(
                context,
                lifecycleOwner = lifecycleOwner
            )
            shadowOf(notificationManager).apply {
                setNotificationsEnabled(true)
                notificationChannels.map { it as NotificationChannel }
                    .find { it.id == "exposure_notifications" }?.importance =
                    NotificationManager.IMPORTANCE_NONE
            }

            assertEquals(false, repository.exposureNotificationsEnabled().first())
        }

    @Test
    fun `exposureNotificationsEnabled refreshes when app is started`() = runBlockingTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val lifecycleOwner = ProcessLifecycleOwner.get()
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val repository = NotificationsRepository(
            context,
            lifecycleOwner = lifecycleOwner
        )
        shadowOf(notificationManager).setNotificationsEnabled(false)

        val result = async { repository.exposureNotificationsEnabled().take(2).toList() }
        yield()

        shadowOf(notificationManager).setNotificationsEnabled(true)
        (lifecycleOwner.lifecycle as LifecycleRegistry).handleLifecycleEvent(Lifecycle.Event.ON_START)

        assertEquals(
            listOf(false, true),
            result.await()
        )
    }
}
