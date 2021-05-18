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
    fun `pausedState is set on init`() = runBlocking {
        Mockito.`when`(settingsRepository.exposureNotificationsPausedState())
            .thenReturn(flowOf(Settings.PausedState.Enabled))

        val settingsViewModel = SettingsViewModel(settingsRepository)

        settingsViewModel.pausedState.observeForTesting {
            Assert.assertEquals(
                Settings.PausedState.Enabled,
                it.values.first()
            )
        }
    }

    @Test
    fun `exposureNotificationsPaused is true when pausedState is Paused`() = runBlocking {
        Mockito.`when`(settingsRepository.exposureNotificationsPausedState())
            .thenReturn(flowOf(Settings.PausedState.Paused(LocalDateTime.now())))

        val settingsViewModel = SettingsViewModel(settingsRepository)

        settingsViewModel.exposureNotificationsPaused.observeForTesting {
            Assert.assertEquals(
                true,
                it.values.first()
            )
        }
    }

    @Test
    fun `wifiOnly is set on init`() = runBlocking {
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
            settingsViewModel.wifiOnly.value = true

            verify(settingsRepository, times(1)).setWifiOnly(true)
            verify(settingsRepository, never()).setWifiOnly(false)

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
}
