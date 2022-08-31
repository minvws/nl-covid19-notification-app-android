/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.config.AppConfigManager
import nl.rijksoverheid.en.enapi.StatusResult
import nl.rijksoverheid.en.notifier.NotificationsRepository
import nl.rijksoverheid.en.onboarding.OnboardingRepository
import nl.rijksoverheid.en.settings.Settings
import nl.rijksoverheid.en.settings.SettingsRepository
import nl.rijksoverheid.en.util.observeForTesting
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class StatusViewModelTest {

    @Mock
    private lateinit var onboardingRepository: OnboardingRepository

    @Mock
    private lateinit var exposureNotificationsRepository: ExposureNotificationsRepository

    @Mock
    private lateinit var notificationsRepository: NotificationsRepository

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    @Mock
    private lateinit var appConfigManager: AppConfigManager

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
    fun `headerState active with valid conditions`() = runBlocking {
        val clock = Clock.fixed(Instant.parse("2020-06-20T10:15:30.00Z"), ZoneId.of("UTC"))

        `when`(exposureNotificationsRepository.getStatus())
            .thenReturn(flowOf(StatusResult.Enabled))
        `when`(settingsRepository.exposureNotificationsPausedState())
            .thenReturn(flowOf(Settings.PausedState.Enabled))
        `when`(exposureNotificationsRepository.lastKeyProcessed())
            .thenReturn(flowOf(clock.millis()))
        `when`(exposureNotificationsRepository.notificationsEnabledTimestamp())
            .thenReturn(flowOf(clock.millis()))
        `when`(exposureNotificationsRepository.getLastExposureDate())
            .thenReturn(flowOf(null))
        `when`(exposureNotificationsRepository.getLastNotificationReceivedDate())
            .thenReturn(null)
        `when`(exposureNotificationsRepository.keyProcessingOverdue())
            .thenReturn(false)
        `when`(notificationsRepository.exposureNotificationChannelEnabled())
            .thenReturn(true)

        val statusViewModel = StatusViewModel(
            onboardingRepository,
            exposureNotificationsRepository,
            notificationsRepository,
            settingsRepository,
            appConfigManager,
            clock
        )

        statusViewModel.headerState.observeForTesting {
            assertEquals(
                StatusViewModel.HeaderState.Active,
                it.values.first()
            )
        }

        assertEquals(emptyList<StatusViewModel.NotificationState>(), statusViewModel.notificationState.first())
    }

    @Test
    fun `headerState exposed with no errorState`() = runBlocking {
        val clock = Clock.fixed(Instant.parse("2020-06-20T10:15:30.00Z"), ZoneId.of("UTC"))
        val exposureDate = LocalDate.now().minusDays(1)
        val notificationReceivedDate = LocalDate.now()
        val pausedState = Settings.PausedState.Enabled

        `when`(exposureNotificationsRepository.getStatus())
            .thenReturn(flowOf(StatusResult.Enabled))
        `when`(settingsRepository.exposureNotificationsPausedState())
            .thenReturn(flowOf(pausedState))
        `when`(exposureNotificationsRepository.lastKeyProcessed())
            .thenReturn(flowOf(clock.millis()))
        `when`(exposureNotificationsRepository.notificationsEnabledTimestamp())
            .thenReturn(flowOf(clock.millis()))
        `when`(exposureNotificationsRepository.getLastExposureDate())
            .thenReturn(flowOf(exposureDate))
        `when`(exposureNotificationsRepository.getLastNotificationReceivedDate())
            .thenReturn(notificationReceivedDate)
        `when`(exposureNotificationsRepository.keyProcessingOverdue())
            .thenReturn(false)
        `when`(notificationsRepository.exposureNotificationChannelEnabled())
            .thenReturn(true)

        val statusViewModel = StatusViewModel(
            onboardingRepository,
            exposureNotificationsRepository,
            notificationsRepository,
            settingsRepository,
            appConfigManager,
            clock
        )

        statusViewModel.headerState.observeForTesting {
            assertEquals(
                StatusViewModel.HeaderState.Exposed(
                    exposureDate,
                    notificationReceivedDate,
                    clock
                ),
                it.values.first()
            )
        }

        assertEquals(emptyList<StatusViewModel.NotificationState>(), statusViewModel.notificationState.first())
    }

    @Test
    fun `NotificationsDisabled without being exposed `() = runBlocking {
        val clock = Clock.fixed(Instant.parse("2020-06-20T10:15:30.00Z"), ZoneId.of("UTC"))

        `when`(exposureNotificationsRepository.getStatus())
            .thenReturn(flowOf(StatusResult.Enabled))
        `when`(settingsRepository.exposureNotificationsPausedState())
            .thenReturn(flowOf(Settings.PausedState.Enabled))
        `when`(exposureNotificationsRepository.lastKeyProcessed())
            .thenReturn(flowOf(clock.millis()))
        `when`(exposureNotificationsRepository.notificationsEnabledTimestamp())
            .thenReturn(flowOf(clock.millis()))
        `when`(exposureNotificationsRepository.getLastExposureDate())
            .thenReturn(flowOf(null))
        `when`(exposureNotificationsRepository.getLastNotificationReceivedDate())
            .thenReturn(null)
        `when`(exposureNotificationsRepository.keyProcessingOverdue())
            .thenReturn(false)
        `when`(notificationsRepository.exposureNotificationChannelEnabled())
            .thenReturn(false)

        val statusViewModel = StatusViewModel(
            onboardingRepository,
            exposureNotificationsRepository,
            notificationsRepository,
            settingsRepository,
            appConfigManager,
            clock
        )

        statusViewModel.headerState.observeForTesting {
            assertEquals(
                StatusViewModel.HeaderState.Active,
                it.values.first()
            )
        }

        assertEquals(listOf(StatusViewModel.NotificationState.Error.NotificationsDisabled), statusViewModel.notificationState.first())
    }

    @Test
    fun `headerState exposed with syncIssues`() = runBlocking {
        val clock = Clock.fixed(Instant.parse("2020-06-20T10:15:30.00Z"), ZoneId.of("UTC"))
        val exposureDate = LocalDate.now().minusDays(1)
        val notificationReceivedDate = LocalDate.now()
        val pausedState = Settings.PausedState.Enabled

        `when`(exposureNotificationsRepository.getStatus())
            .thenReturn(flowOf(StatusResult.Enabled))
        `when`(settingsRepository.exposureNotificationsPausedState())
            .thenReturn(flowOf(pausedState))
        `when`(exposureNotificationsRepository.lastKeyProcessed())
            .thenReturn(flowOf(clock.millis()))
        `when`(exposureNotificationsRepository.notificationsEnabledTimestamp())
            .thenReturn(flowOf(clock.millis()))
        `when`(exposureNotificationsRepository.getLastExposureDate())
            .thenReturn(flowOf(exposureDate))
        `when`(exposureNotificationsRepository.getLastNotificationReceivedDate())
            .thenReturn(notificationReceivedDate)
        `when`(exposureNotificationsRepository.keyProcessingOverdue())
            .thenReturn(true)
        `when`(notificationsRepository.exposureNotificationChannelEnabled())
            .thenReturn(true)

        val statusViewModel = StatusViewModel(
            onboardingRepository,
            exposureNotificationsRepository,
            notificationsRepository,
            settingsRepository,
            appConfigManager,
            clock
        )

        statusViewModel.headerState.observeForTesting {
            assertEquals(
                StatusViewModel.HeaderState.Exposed(
                    exposureDate,
                    notificationReceivedDate,
                    clock
                ),
                it.values.first()
            )
        }

        assertEquals(listOf<StatusViewModel.NotificationState>(StatusViewModel.NotificationState.Error.SyncIssues), statusViewModel.notificationState.first())
    }

    @Test
    fun `headerState exposed and notificationState paused when framework is paused while being exposed`() =
        runBlocking {
            val clock = Clock.fixed(Instant.parse("2020-06-20T10:15:30.00Z"), ZoneId.of("UTC"))
            val exposureDate = LocalDate.now(clock).minusDays(1)
            val notificationReceivedDate = LocalDate.now(clock)
            val pausedState = Settings.PausedState.Paused(LocalDateTime.now(clock).plusDays(1))

            `when`(exposureNotificationsRepository.getStatus())
                .thenReturn(flowOf(StatusResult.Disabled))
            `when`(settingsRepository.exposureNotificationsPausedState())
                .thenReturn(flowOf(pausedState))
            `when`(exposureNotificationsRepository.lastKeyProcessed())
                .thenReturn(flowOf(clock.millis()))
            `when`(exposureNotificationsRepository.notificationsEnabledTimestamp())
                .thenReturn(flowOf(clock.millis()))
            `when`(exposureNotificationsRepository.getLastExposureDate())
                .thenReturn(flowOf(exposureDate))
            `when`(exposureNotificationsRepository.getLastNotificationReceivedDate())
                .thenReturn(notificationReceivedDate)
            `when`(exposureNotificationsRepository.keyProcessingOverdue())
                .thenReturn(true)
            `when`(notificationsRepository.exposureNotificationChannelEnabled())
                .thenReturn(true)

            val statusViewModel = StatusViewModel(
                onboardingRepository,
                exposureNotificationsRepository,
                notificationsRepository,
                settingsRepository,
                appConfigManager,
                clock
            )

            statusViewModel.headerState.observeForTesting {
                assertEquals(
                    StatusViewModel.HeaderState.Exposed(
                        exposureDate,
                        notificationReceivedDate,
                        clock
                    ),
                    it.values.first()
                )
            }

            assertEquals(
                listOf<StatusViewModel.NotificationState>(
                    StatusViewModel.NotificationState.Paused(
                        pausedState.pausedUntil
                    )
                ),
                statusViewModel.notificationState.first()
            )
        }

    @Test
    fun `headerState exposed with errorState consentRequired`() = runBlocking {
        val clock = Clock.fixed(Instant.parse("2020-06-20T10:15:30.00Z"), ZoneId.of("UTC"))
        val exposureDate = LocalDate.now(clock).minusDays(1)
        val notificationReceivedDate = LocalDate.now(clock)
        val pausedState = Settings.PausedState.Enabled

        `when`(exposureNotificationsRepository.getStatus())
            .thenReturn(flowOf(StatusResult.Disabled))
        `when`(settingsRepository.exposureNotificationsPausedState())
            .thenReturn(flowOf(pausedState))
        `when`(exposureNotificationsRepository.lastKeyProcessed())
            .thenReturn(flowOf(clock.millis()))
        `when`(exposureNotificationsRepository.notificationsEnabledTimestamp())
            .thenReturn(flowOf(clock.millis()))
        `when`(exposureNotificationsRepository.getLastExposureDate())
            .thenReturn(flowOf(exposureDate))
        `when`(exposureNotificationsRepository.getLastNotificationReceivedDate())
            .thenReturn(notificationReceivedDate)
        `when`(exposureNotificationsRepository.keyProcessingOverdue())
            .thenReturn(false)
        `when`(notificationsRepository.exposureNotificationChannelEnabled())
            .thenReturn(true)

        val statusViewModel = StatusViewModel(
            onboardingRepository,
            exposureNotificationsRepository,
            notificationsRepository,
            settingsRepository,
            appConfigManager,
            clock
        )

        statusViewModel.headerState.observeForTesting {
            assertEquals(
                StatusViewModel.HeaderState.Exposed(
                    exposureDate,
                    notificationReceivedDate,
                    clock
                ),
                it.values.first()
            )
        }

        assertEquals(listOf<StatusViewModel.NotificationState>(StatusViewModel.NotificationState.Error.ConsentRequired), statusViewModel.notificationState.first())
    }

    @Test
    fun `headerState bluetooth disabled`() = runBlocking {
        val clock = Clock.fixed(Instant.parse("2020-06-20T10:15:30.00Z"), ZoneId.of("UTC"))

        `when`(exposureNotificationsRepository.getStatus())
            .thenReturn(flowOf(StatusResult.BluetoothDisabled))
        `when`(settingsRepository.exposureNotificationsPausedState())
            .thenReturn(flowOf(Settings.PausedState.Enabled))
        `when`(exposureNotificationsRepository.lastKeyProcessed())
            .thenReturn(flowOf(clock.millis()))
        `when`(exposureNotificationsRepository.notificationsEnabledTimestamp())
            .thenReturn(flowOf(clock.millis()))
        `when`(exposureNotificationsRepository.getLastExposureDate())
            .thenReturn(flowOf(null))
        `when`(exposureNotificationsRepository.getLastNotificationReceivedDate())
            .thenReturn(null)
        `when`(exposureNotificationsRepository.keyProcessingOverdue())
            .thenReturn(false)
        `when`(notificationsRepository.exposureNotificationChannelEnabled())
            .thenReturn(true)

        val statusViewModel = StatusViewModel(
            onboardingRepository,
            exposureNotificationsRepository,
            notificationsRepository,
            settingsRepository,
            appConfigManager,
            clock
        )

        statusViewModel.headerState.observeForTesting {
            assertEquals(
                StatusViewModel.HeaderState.BluetoothDisabled,
                it.values.first()
            )
        }

        assertEquals(emptyList<StatusViewModel.NotificationState>(), statusViewModel.notificationState.first())
    }

    @Test
    fun `headerState syncIssues wifi only`() = runBlocking {
        val clock = Clock.fixed(Instant.parse("2020-06-20T10:15:30.00Z"), ZoneId.of("UTC"))

        `when`(exposureNotificationsRepository.getStatus())
            .thenReturn(flowOf(StatusResult.Enabled))
        `when`(settingsRepository.exposureNotificationsPausedState())
            .thenReturn(flowOf(Settings.PausedState.Enabled))
        `when`(exposureNotificationsRepository.lastKeyProcessed())
            .thenReturn(flowOf(clock.millis()))
        `when`(exposureNotificationsRepository.notificationsEnabledTimestamp())
            .thenReturn(flowOf(clock.millis()))
        `when`(exposureNotificationsRepository.getLastExposureDate())
            .thenReturn(flowOf(null))
        `when`(exposureNotificationsRepository.getLastNotificationReceivedDate())
            .thenReturn(null)
        `when`(exposureNotificationsRepository.keyProcessingOverdue())
            .thenReturn(true)
        `when`(notificationsRepository.exposureNotificationChannelEnabled())
            .thenReturn(true)
        `when`(settingsRepository.wifiOnly)
            .thenReturn(true)

        val statusViewModel = StatusViewModel(
            onboardingRepository,
            exposureNotificationsRepository,
            notificationsRepository,
            settingsRepository,
            appConfigManager,
            clock
        )

        statusViewModel.headerState.observeForTesting {
            assertEquals(
                StatusViewModel.HeaderState.SyncIssuesWifiOnly,
                it.values.first()
            )
        }

        assertEquals(emptyList<StatusViewModel.NotificationState>(), statusViewModel.notificationState.first())
    }

    @Test
    fun `headerState disabled when keyProcessingOverdue and locationPreconditionNotSatisfied`() =
        runBlocking {
            val clock = Clock.fixed(Instant.parse("2020-06-20T10:15:30.00Z"), ZoneId.of("UTC"))

            `when`(exposureNotificationsRepository.getStatus())
                .thenReturn(flowOf(StatusResult.LocationPreconditionNotSatisfied))
            `when`(settingsRepository.exposureNotificationsPausedState())
                .thenReturn(flowOf(Settings.PausedState.Enabled))
            `when`(exposureNotificationsRepository.lastKeyProcessed())
                .thenReturn(flowOf(clock.millis()))
            `when`(exposureNotificationsRepository.notificationsEnabledTimestamp())
                .thenReturn(flowOf(clock.millis()))
            `when`(exposureNotificationsRepository.getLastExposureDate())
                .thenReturn(flowOf(null))
            `when`(exposureNotificationsRepository.getLastNotificationReceivedDate())
                .thenReturn(null)
            `when`(exposureNotificationsRepository.keyProcessingOverdue())
                .thenReturn(true)
            `when`(notificationsRepository.exposureNotificationChannelEnabled())
                .thenReturn(true)

            val statusViewModel = StatusViewModel(
                onboardingRepository,
                exposureNotificationsRepository,
                notificationsRepository,
                settingsRepository,
                appConfigManager,
                clock
            )

            statusViewModel.headerState.observeForTesting {
                assertEquals(
                    StatusViewModel.HeaderState.Disabled,
                    it.values.first()
                )
            }

            assertEquals(emptyList<StatusViewModel.NotificationState>(), statusViewModel.notificationState.first())
        }

    @Test
    fun `resetErrorState calls rescheduleBackgroundJobs and resetNotificationsEnabledTimestamp`() {
        runBlocking {
            val clock = Clock.fixed(Instant.parse("2020-06-20T10:15:30.00Z"), ZoneId.of("UTC"))

            `when`(exposureNotificationsRepository.getStatus())
                .thenReturn(flowOf(StatusResult.Enabled))
            `when`(settingsRepository.exposureNotificationsPausedState())
                .thenReturn(flowOf(Settings.PausedState.Enabled))
            `when`(exposureNotificationsRepository.lastKeyProcessed())
                .thenReturn(flowOf(clock.millis()))
            `when`(exposureNotificationsRepository.notificationsEnabledTimestamp())
                .thenReturn(flowOf(clock.millis()))
            `when`(exposureNotificationsRepository.getLastExposureDate())
                .thenReturn(flowOf(null))
            `when`(exposureNotificationsRepository.getLastNotificationReceivedDate())
                .thenReturn(null)
            `when`(exposureNotificationsRepository.keyProcessingOverdue())
                .thenReturn(false)
            `when`(notificationsRepository.exposureNotificationChannelEnabled())
                .thenReturn(true)

            val statusViewModel = StatusViewModel(
                onboardingRepository,
                exposureNotificationsRepository,
                notificationsRepository,
                settingsRepository,
                appConfigManager,
                clock
            )

            statusViewModel.resetErrorState()

            verify(exposureNotificationsRepository, times(1)).resetNotificationsEnabledTimestamp()
            verify(exposureNotificationsRepository, times(1)).rescheduleBackgroundJobs()
        }
    }
}
