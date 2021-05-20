/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import android.app.PendingIntent
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.en.enapi.EnableNotificationsResult
import nl.rijksoverheid.en.enapi.StatusResult
import nl.rijksoverheid.en.settings.SettingsRepository
import nl.rijksoverheid.en.util.observeForTesting
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExposureNotificationsViewModelTest {

    @Mock
    private lateinit var exposureNotificationsRepository: ExposureNotificationsRepository

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    private lateinit var closeable: AutoCloseable

    @Before
    fun openMocks() {
        closeable = MockitoAnnotations.openMocks(this)
    }

    @After
    fun releaseMocks() {
        closeable.close()
    }

    @Test
    fun `cleanupPreviouslyKnownExposures is called on init`() = runBlocking {
        Mockito.`when`(exposureNotificationsRepository.getStatus())
            .thenReturn(flowOf(StatusResult.Enabled))

        ExposureNotificationsViewModel(
            exposureNotificationsRepository,
            settingsRepository
        )

        verify(exposureNotificationsRepository, times(1)).cleanupPreviouslyKnownExposures()
    }

    @Test
    fun `notificationState available on init`() = runBlocking {
        Mockito.`when`(exposureNotificationsRepository.getStatus())
            .thenReturn(flowOf(StatusResult.Enabled))

        val exposureNotificationsViewModel = ExposureNotificationsViewModel(
            exposureNotificationsRepository,
            settingsRepository
        )

        exposureNotificationsViewModel.notificationState.observeForTesting {
            Assert.assertEquals(
                ExposureNotificationsViewModel.NotificationsState.Enabled,
                it.values.first()
            )
        }
    }

    @Test
    fun `requestEnableNotifications calls clearExposureNotificationsPaused on settingsRepository`() {
        runBlocking {
            Mockito.`when`(exposureNotificationsRepository.getStatus())
                .thenReturn(flowOf(StatusResult.Disabled))
            Mockito.`when`(exposureNotificationsRepository.requestEnableNotifications())
                .thenReturn(EnableNotificationsResult.Enabled)

            val exposureNotificationsViewModel = ExposureNotificationsViewModel(
                exposureNotificationsRepository,
                settingsRepository
            )

            exposureNotificationsViewModel.requestEnableNotifications()

            verify(settingsRepository, times(1)).clearExposureNotificationsPaused()
        }
    }

    @Test
    fun `requestEnableNotificationsForcingConsent consent required`() = runBlocking {
        val pendingIntent: PendingIntent = mock(PendingIntent::class.java)

        Mockito.`when`(exposureNotificationsRepository.getStatus())
            .thenReturn(flowOf(StatusResult.Disabled))
        Mockito.`when`(exposureNotificationsRepository.requestEnableNotificationsForcingConsent())
            .thenReturn(EnableNotificationsResult.ResolutionRequired(pendingIntent))

        val exposureNotificationsViewModel = ExposureNotificationsViewModel(
            exposureNotificationsRepository,
            settingsRepository
        )

        exposureNotificationsViewModel.requestEnableNotificationsForcingConsent()

        verify(settingsRepository, never()).clearExposureNotificationsPaused()

        exposureNotificationsViewModel.notificationsResult.observeForTesting {
            Assert.assertEquals(
                ExposureNotificationsViewModel.NotificationsStatusResult.ConsentRequired(pendingIntent),
                it.values.first().getContentIfNotHandled()
            )
        }
    }

    @Test
    fun `rescheduleBackgroundJobs without resetNotificationsEnabledTimestamp`() {
        runBlocking {
            Mockito.`when`(exposureNotificationsRepository.getStatus())
                .thenReturn(flowOf(StatusResult.Disabled))
            Mockito.`when`(exposureNotificationsRepository.keyProcessingOverdue())
                .thenReturn(false)
            Mockito.`when`(exposureNotificationsRepository.rescheduleBackgroundJobs())
                .thenReturn(Unit)

            val exposureNotificationsViewModel = ExposureNotificationsViewModel(
                exposureNotificationsRepository,
                settingsRepository
            )

            exposureNotificationsViewModel.rescheduleBackgroundJobs()

            verify(exposureNotificationsRepository, never()).resetNotificationsEnabledTimestamp()
            verify(exposureNotificationsRepository, times(1)).rescheduleBackgroundJobs()
        }
    }
}
