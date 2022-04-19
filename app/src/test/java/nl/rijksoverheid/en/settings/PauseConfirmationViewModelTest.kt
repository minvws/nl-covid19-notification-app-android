/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.settings

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class PauseConfirmationViewModelTest {

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
    fun `SkipPauseConfirmation isn't being updated when false`() {
        val pauseConfirmationViewModel = PauseConfirmationViewModel(settingsRepository)
        val until = LocalDateTime.now()

        pauseConfirmationViewModel.setExposureNotificationsPaused(until)

        verify(settingsRepository, never()).skipPauseConfirmation = true
        verify(settingsRepository, times(1)).setExposureNotificationsPaused(until)
    }

    @Test
    fun `SkipPauseConfirmation is being updated when true`() {
        val pauseConfirmationViewModel = PauseConfirmationViewModel(settingsRepository)
        val until = LocalDateTime.now()

        pauseConfirmationViewModel.toggleDontAskForConfirmation()
        pauseConfirmationViewModel.setExposureNotificationsPaused(until)

        verify(settingsRepository, times(1)).skipPauseConfirmation = true
        verify(settingsRepository, times(1)).setExposureNotificationsPaused(until)
    }
}
