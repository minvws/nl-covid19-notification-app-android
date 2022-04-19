/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.settings

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.en.util.observeForTesting
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

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
    fun `pausedState available on init`() = runBlocking {
        val pausedState = Settings.PausedState.Paused(LocalDateTime.now())
        Mockito.`when`(settingsRepository.exposureNotificationsPausedState())
            .thenReturn(flowOf(pausedState))

        val settingsViewModel = SettingsViewModel(settingsRepository)

        settingsViewModel.pausedState.observeForTesting {
            Assert.assertEquals(
                pausedState,
                it.values.first()
            )
        }
    }

    @Test
    fun `exposureNotificationsPaused is pausedState when Paused`() = runBlocking {
        val pausedState = Settings.PausedState.Paused(LocalDateTime.now())
        Mockito.`when`(settingsRepository.exposureNotificationsPausedState())
            .thenReturn(flowOf(pausedState))

        val settingsViewModel = SettingsViewModel(settingsRepository)

        settingsViewModel.pausedState.observeForTesting {
            Assert.assertEquals(
                pausedState,
                it.values.first()
            )
        }
    }

    @Test
    fun `exposureNotificationsPaused is null when Enabled`() = runBlocking {
        val pausedState = Settings.PausedState.Enabled
        Mockito.`when`(settingsRepository.exposureNotificationsPausedState())
            .thenReturn(flowOf(pausedState))

        val settingsViewModel = SettingsViewModel(settingsRepository)

        settingsViewModel.pausedState.observeForTesting {
            Assert.assertEquals(
                null,
                it.values.first()
            )
        }
    }

    @Test
    fun `wifiOnly available on init`() = runBlocking {
        Mockito.`when`(settingsRepository.exposureNotificationsPausedState())
            .thenReturn(flowOf(Settings.PausedState.Enabled))
        Mockito.`when`(settingsRepository.wifiOnly)
            .thenReturn(true)

        val settingsViewModel = SettingsViewModel(settingsRepository)

        settingsViewModel.wifiOnly.observeForTesting {
            Assert.assertTrue(
                it.values.first()
            )
        }
    }

    @Test
    fun `Setting wifiOnly persists state to repository after initialization and triggers wifiOnlyChanged`() {
        runBlocking {
            Mockito.`when`(settingsRepository.exposureNotificationsPausedState())
                .thenReturn(flowOf(Settings.PausedState.Enabled))
            Mockito.`when`(settingsRepository.wifiOnly)
                .thenReturn(false)

            val settingsViewModel = SettingsViewModel(settingsRepository)
            settingsViewModel.wifiOnlyChanged(true)

            verify(settingsRepository, times(1)).wifiOnly = true
            verify(settingsRepository, never()).wifiOnly = false

            settingsViewModel.wifiOnlyChanged.observeForTesting {
                Assert.assertEquals(
                    true,
                    it.values.first().getContentIfNotHandled()
                )
            }
        }
    }

    @Test
    fun `requestPauseExposureNotifications triggers pauseRequested`() {
        runBlocking {
            Mockito.`when`(settingsRepository.exposureNotificationsPausedState())
                .thenReturn(flowOf(Settings.PausedState.Enabled))
            Mockito.`when`(settingsRepository.wifiOnly)
                .thenReturn(false)

            val settingsViewModel = SettingsViewModel(settingsRepository)
            settingsViewModel.requestPauseExposureNotifications()

            settingsViewModel.pauseRequested.observeForTesting {
                Assert.assertEquals(
                    1,
                    it.values.size
                )
            }
        }
    }

    @Test
    fun `enableExposureNotifications triggers enableExposureNotificationsRequested`() {
        runBlocking {
            Mockito.`when`(settingsRepository.exposureNotificationsPausedState())
                .thenReturn(flowOf(Settings.PausedState.Enabled))
            Mockito.`when`(settingsRepository.wifiOnly)
                .thenReturn(false)

            val settingsViewModel = SettingsViewModel(settingsRepository)
            settingsViewModel.enableExposureNotifications()

            settingsViewModel.enableExposureNotificationsRequested.observeForTesting {
                Assert.assertEquals(
                    1,
                    it.values.size
                )
            }
        }
    }

    @Test
    fun `setExposureNotificationsPaused passes paused state to repository`() {
        runBlocking {
            Mockito.`when`(settingsRepository.exposureNotificationsPausedState())
                .thenReturn(flowOf(Settings.PausedState.Enabled))
            Mockito.`when`(settingsRepository.wifiOnly)
                .thenReturn(false)

            val settingsViewModel = SettingsViewModel(settingsRepository)
            val pausedUntil = LocalDateTime.now().plusDays(1)
            settingsViewModel.setExposureNotificationsPaused(pausedUntil)

            verify(settingsRepository, times(1))
                .setExposureNotificationsPaused(pausedUntil)
        }
    }

    @Test
    fun `dashboardEnabled available on init`() = runBlocking {
        Mockito.`when`(settingsRepository.dashboardEnabled)
            .thenReturn(true)

        val settingsViewModel = SettingsViewModel(settingsRepository)

        settingsViewModel.dashboardEnabled.observeForTesting {
            Assert.assertTrue(
                it.values.first()
            )
        }
    }

    @Test
    fun `Setting setDashboardEnabled persists state to repository`() {
        runBlocking {
            Mockito.`when`(settingsRepository.dashboardEnabled)
                .thenReturn(true)

            val settingsViewModel = SettingsViewModel(settingsRepository)
            settingsViewModel.dashboardEnabledChanged(false)

            verify(settingsRepository, times(1)).dashboardEnabled = false
            verify(settingsRepository, never()).dashboardEnabled = true
        }
    }
}
