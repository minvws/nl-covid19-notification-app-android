/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.config.AppConfigManager
import nl.rijksoverheid.en.dashboard.DashboardRepository
import nl.rijksoverheid.en.dashboardTestData
import nl.rijksoverheid.en.enapi.StatusResult
import nl.rijksoverheid.en.notifier.NotificationsRepository
import nl.rijksoverheid.en.onboarding.OnboardingRepository
import nl.rijksoverheid.en.settings.Settings
import nl.rijksoverheid.en.settings.SettingsRepository
import nl.rijksoverheid.en.util.Resource
import nl.rijksoverheid.en.util.observeForTesting
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
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
    private lateinit var dashboardRepository: DashboardRepository

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    @Mock
    private lateinit var appConfigManager: AppConfigManager

    private lateinit var closeable: AutoCloseable

    private val clock = Clock.fixed(Instant.parse("2020-06-20T10:15:30.00Z"), ZoneId.of("UTC"))

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
        Mockito.`when`(exposureNotificationsRepository.getStatus())
            .thenReturn(flowOf(StatusResult.Enabled))
        Mockito.`when`(settingsRepository.exposureNotificationsPausedState())
            .thenReturn(flowOf(Settings.PausedState.Enabled))
        Mockito.`when`(exposureNotificationsRepository.lastKeyProcessed())
            .thenReturn(flowOf(clock.millis()))
        Mockito.`when`(exposureNotificationsRepository.notificationsEnabledTimestamp())
            .thenReturn(flowOf(clock.millis()))
        Mockito.`when`(exposureNotificationsRepository.getLastExposureDate())
            .thenReturn(flowOf(null))
        Mockito.`when`(exposureNotificationsRepository.getLastNotificationReceivedDate())
            .thenReturn(null)
        Mockito.`when`(exposureNotificationsRepository.keyProcessingOverdue())
            .thenReturn(false)
        Mockito.`when`(notificationsRepository.exposureNotificationsEnabled())
            .thenReturn(flowOf(true))

        val statusViewModel = StatusViewModel(
            onboardingRepository,
            exposureNotificationsRepository,
            notificationsRepository,
            dashboardRepository,
            settingsRepository,
            appConfigManager,
            clock
        )

        statusViewModel.headerState.observeForTesting {
            Assert.assertEquals(
                StatusViewModel.HeaderState.Active,
                it.values.first()
            )
        }

        statusViewModel.notificationState.observeForTesting {
            Assert.assertEquals(
                emptyList<StatusViewModel.NotificationState>(),
                it.values.first()
            )
        }
    }

    @Test
    fun `headerState exposed with no errorState`() = runBlocking {
        val exposureDate = LocalDate.now().minusDays(1)
        val notificationReceivedDate = LocalDate.now()
        val pausedState = Settings.PausedState.Enabled

        Mockito.`when`(exposureNotificationsRepository.getStatus())
            .thenReturn(flowOf(StatusResult.Enabled))
        Mockito.`when`(settingsRepository.exposureNotificationsPausedState())
            .thenReturn(flowOf(pausedState))
        Mockito.`when`(exposureNotificationsRepository.lastKeyProcessed())
            .thenReturn(flowOf(clock.millis()))
        Mockito.`when`(exposureNotificationsRepository.notificationsEnabledTimestamp())
            .thenReturn(flowOf(clock.millis()))
        Mockito.`when`(exposureNotificationsRepository.getLastExposureDate())
            .thenReturn(flowOf(exposureDate))
        Mockito.`when`(exposureNotificationsRepository.getLastNotificationReceivedDate())
            .thenReturn(notificationReceivedDate)
        Mockito.`when`(exposureNotificationsRepository.keyProcessingOverdue())
            .thenReturn(false)
        Mockito.`when`(notificationsRepository.exposureNotificationsEnabled())
            .thenReturn(flowOf(true))

        val statusViewModel = StatusViewModel(
            onboardingRepository,
            exposureNotificationsRepository,
            notificationsRepository,
            dashboardRepository,
            settingsRepository,
            appConfigManager,
            clock
        )

        statusViewModel.headerState.observeForTesting {
            Assert.assertEquals(
                StatusViewModel.HeaderState.Exposed(
                    exposureDate,
                    notificationReceivedDate,
                    clock
                ),
                it.values.first()
            )
        }

        statusViewModel.notificationState.observeForTesting {
            Assert.assertEquals(
                emptyList<StatusViewModel.NotificationState>(),
                it.values.first()
            )
        }
    }

    @Test
    fun `NotificationsDisabled without being exposed `() = runBlocking {
        val notificationsEnabled = false

        Mockito.`when`(exposureNotificationsRepository.getStatus())
            .thenReturn(flowOf(StatusResult.Enabled))
        Mockito.`when`(settingsRepository.exposureNotificationsPausedState())
            .thenReturn(flowOf(Settings.PausedState.Enabled))
        Mockito.`when`(exposureNotificationsRepository.lastKeyProcessed())
            .thenReturn(flowOf(clock.millis()))
        Mockito.`when`(exposureNotificationsRepository.notificationsEnabledTimestamp())
            .thenReturn(flowOf(clock.millis()))
        Mockito.`when`(exposureNotificationsRepository.getLastExposureDate())
            .thenReturn(flowOf(null))
        Mockito.`when`(exposureNotificationsRepository.getLastNotificationReceivedDate())
            .thenReturn(null)
        Mockito.`when`(exposureNotificationsRepository.keyProcessingOverdue())
            .thenReturn(false)
        Mockito.`when`(notificationsRepository.exposureNotificationsEnabled())
            .thenReturn(flowOf(notificationsEnabled))

        val statusViewModel = StatusViewModel(
            onboardingRepository,
            exposureNotificationsRepository,
            notificationsRepository,
            dashboardRepository,
            settingsRepository,
            appConfigManager,
            clock
        )

        statusViewModel.headerState.observeForTesting {
            Assert.assertEquals(
                StatusViewModel.HeaderState.Active,
                it.values.first()
            )
        }

        statusViewModel.notificationState.observeForTesting {
            Assert.assertEquals(
                listOf(StatusViewModel.NotificationState.Error.NotificationsDisabled),
                it.values.first()
            )
        }
    }

    @Test
    fun `headerState exposed with syncIssues`() = runBlocking {
        val exposureDate = LocalDate.now().minusDays(1)
        val notificationReceivedDate = LocalDate.now()
        val pausedState = Settings.PausedState.Enabled

        Mockito.`when`(exposureNotificationsRepository.getStatus())
            .thenReturn(flowOf(StatusResult.Enabled))
        Mockito.`when`(settingsRepository.exposureNotificationsPausedState())
            .thenReturn(flowOf(pausedState))
        Mockito.`when`(exposureNotificationsRepository.lastKeyProcessed())
            .thenReturn(flowOf(clock.millis()))
        Mockito.`when`(exposureNotificationsRepository.notificationsEnabledTimestamp())
            .thenReturn(flowOf(clock.millis()))
        Mockito.`when`(exposureNotificationsRepository.getLastExposureDate())
            .thenReturn(flowOf(exposureDate))
        Mockito.`when`(exposureNotificationsRepository.getLastNotificationReceivedDate())
            .thenReturn(notificationReceivedDate)
        Mockito.`when`(exposureNotificationsRepository.keyProcessingOverdue())
            .thenReturn(true)
        Mockito.`when`(notificationsRepository.exposureNotificationsEnabled())
            .thenReturn(flowOf(true))

        val statusViewModel = StatusViewModel(
            onboardingRepository,
            exposureNotificationsRepository,
            notificationsRepository,
            dashboardRepository,
            settingsRepository,
            appConfigManager,
            clock
        )

        statusViewModel.headerState.observeForTesting {
            Assert.assertEquals(
                StatusViewModel.HeaderState.Exposed(
                    exposureDate,
                    notificationReceivedDate,
                    clock
                ),
                it.values.first()
            )
        }

        statusViewModel.notificationState.observeForTesting {
            Assert.assertEquals(
                listOf<StatusViewModel.NotificationState>(StatusViewModel.NotificationState.Error.SyncIssues),
                it.values.first()
            )
        }
    }

    @Test
    fun `headerState exposed and notificationState paused when framework is paused while being exposed`() =
        runBlocking {
            val exposureDate = LocalDate.now(clock).minusDays(1)
            val notificationReceivedDate = LocalDate.now(clock)
            val pausedState = Settings.PausedState.Paused(LocalDateTime.now(clock).plusDays(1))

            Mockito.`when`(exposureNotificationsRepository.getStatus())
                .thenReturn(flowOf(StatusResult.Disabled))
            Mockito.`when`(settingsRepository.exposureNotificationsPausedState())
                .thenReturn(flowOf(pausedState))
            Mockito.`when`(exposureNotificationsRepository.lastKeyProcessed())
                .thenReturn(flowOf(clock.millis()))
            Mockito.`when`(exposureNotificationsRepository.notificationsEnabledTimestamp())
                .thenReturn(flowOf(clock.millis()))
            Mockito.`when`(exposureNotificationsRepository.getLastExposureDate())
                .thenReturn(flowOf(exposureDate))
            Mockito.`when`(exposureNotificationsRepository.getLastNotificationReceivedDate())
                .thenReturn(notificationReceivedDate)
            Mockito.`when`(exposureNotificationsRepository.keyProcessingOverdue())
                .thenReturn(true)
            Mockito.`when`(notificationsRepository.exposureNotificationsEnabled())
                .thenReturn(flowOf(true))

            val statusViewModel = StatusViewModel(
                onboardingRepository,
                exposureNotificationsRepository,
                notificationsRepository,
                dashboardRepository,
                settingsRepository,
                appConfigManager,
                clock
            )

            statusViewModel.headerState.observeForTesting {
                Assert.assertEquals(
                    StatusViewModel.HeaderState.Exposed(
                        exposureDate,
                        notificationReceivedDate,
                        clock
                    ),
                    it.values.first()
                )
            }

            statusViewModel.notificationState.observeForTesting {
                Assert.assertEquals(
                    listOf<StatusViewModel.NotificationState>(
                        StatusViewModel.NotificationState.Paused(
                            pausedState.pausedUntil
                        )
                    ),
                    it.values.first()
                )
            }
        }

    @Test
    fun `headerState exposed with errorState consentRequired`() = runBlocking {
        val exposureDate = LocalDate.now(clock).minusDays(1)
        val notificationReceivedDate = LocalDate.now(clock)
        val pausedState = Settings.PausedState.Enabled

        Mockito.`when`(exposureNotificationsRepository.getStatus())
            .thenReturn(flowOf(StatusResult.Disabled))
        Mockito.`when`(settingsRepository.exposureNotificationsPausedState())
            .thenReturn(flowOf(pausedState))
        Mockito.`when`(exposureNotificationsRepository.lastKeyProcessed())
            .thenReturn(flowOf(clock.millis()))
        Mockito.`when`(exposureNotificationsRepository.notificationsEnabledTimestamp())
            .thenReturn(flowOf(clock.millis()))
        Mockito.`when`(exposureNotificationsRepository.getLastExposureDate())
            .thenReturn(flowOf(exposureDate))
        Mockito.`when`(exposureNotificationsRepository.getLastNotificationReceivedDate())
            .thenReturn(notificationReceivedDate)
        Mockito.`when`(exposureNotificationsRepository.keyProcessingOverdue())
            .thenReturn(false)
        Mockito.`when`(notificationsRepository.exposureNotificationsEnabled())
            .thenReturn(flowOf(true))

        val statusViewModel = StatusViewModel(
            onboardingRepository,
            exposureNotificationsRepository,
            notificationsRepository,
            dashboardRepository,
            settingsRepository,
            appConfigManager,
            clock
        )

        statusViewModel.headerState.observeForTesting {
            Assert.assertEquals(
                StatusViewModel.HeaderState.Exposed(
                    exposureDate,
                    notificationReceivedDate,
                    clock
                ),
                it.values.first()
            )
        }

        statusViewModel.notificationState.observeForTesting {
            Assert.assertEquals(
                listOf<StatusViewModel.NotificationState>(StatusViewModel.NotificationState.Error.ConsentRequired),
                it.values.first()
            )
        }
    }

    @Test
    fun `headerState bluetooth disabled`() = runBlocking {

        Mockito.`when`(exposureNotificationsRepository.getStatus())
            .thenReturn(flowOf(StatusResult.BluetoothDisabled))
        Mockito.`when`(settingsRepository.exposureNotificationsPausedState())
            .thenReturn(flowOf(Settings.PausedState.Enabled))
        Mockito.`when`(exposureNotificationsRepository.lastKeyProcessed())
            .thenReturn(flowOf(clock.millis()))
        Mockito.`when`(exposureNotificationsRepository.notificationsEnabledTimestamp())
            .thenReturn(flowOf(clock.millis()))
        Mockito.`when`(exposureNotificationsRepository.getLastExposureDate())
            .thenReturn(flowOf(null))
        Mockito.`when`(exposureNotificationsRepository.getLastNotificationReceivedDate())
            .thenReturn(null)
        Mockito.`when`(exposureNotificationsRepository.keyProcessingOverdue())
            .thenReturn(false)
        Mockito.`when`(notificationsRepository.exposureNotificationsEnabled())
            .thenReturn(flowOf(true))

        val statusViewModel = StatusViewModel(
            onboardingRepository,
            exposureNotificationsRepository,
            notificationsRepository,
            dashboardRepository,
            settingsRepository,
            appConfigManager,
            clock
        )

        statusViewModel.headerState.observeForTesting {
            Assert.assertEquals(
                StatusViewModel.HeaderState.BluetoothDisabled,
                it.values.first()
            )
        }

        statusViewModel.notificationState.observeForTesting {
            Assert.assertEquals(
                emptyList<StatusViewModel.NotificationState>(),
                it.values.first()
            )
        }
    }

    @Test
    fun `headerState syncIssues wifi only`() = runBlocking {

        Mockito.`when`(exposureNotificationsRepository.getStatus())
            .thenReturn(flowOf(StatusResult.Enabled))
        Mockito.`when`(settingsRepository.exposureNotificationsPausedState())
            .thenReturn(flowOf(Settings.PausedState.Enabled))
        Mockito.`when`(exposureNotificationsRepository.lastKeyProcessed())
            .thenReturn(flowOf(clock.millis()))
        Mockito.`when`(exposureNotificationsRepository.notificationsEnabledTimestamp())
            .thenReturn(flowOf(clock.millis()))
        Mockito.`when`(exposureNotificationsRepository.getLastExposureDate())
            .thenReturn(flowOf(null))
        Mockito.`when`(exposureNotificationsRepository.getLastNotificationReceivedDate())
            .thenReturn(null)
        Mockito.`when`(exposureNotificationsRepository.keyProcessingOverdue())
            .thenReturn(true)
        Mockito.`when`(notificationsRepository.exposureNotificationsEnabled())
            .thenReturn(flowOf(true))
        Mockito.`when`(settingsRepository.wifiOnly)
            .thenReturn(true)

        val statusViewModel = StatusViewModel(
            onboardingRepository,
            exposureNotificationsRepository,
            notificationsRepository,
            dashboardRepository,
            settingsRepository,
            appConfigManager,
            clock
        )

        statusViewModel.headerState.observeForTesting {
            Assert.assertEquals(
                StatusViewModel.HeaderState.SyncIssuesWifiOnly,
                it.values.first()
            )
        }

        statusViewModel.notificationState.observeForTesting {
            Assert.assertEquals(
                emptyList<StatusViewModel.NotificationState>(),
                it.values.first()
            )
        }
    }

    @Test
    fun `headerState disabled when keyProcessingOverdue and locationPreconditionNotSatisfied`() =
        runBlocking {
            val clock = Clock.fixed(Instant.parse("2020-06-20T10:15:30.00Z"), ZoneId.of("UTC"))

            Mockito.`when`(exposureNotificationsRepository.getStatus())
                .thenReturn(flowOf(StatusResult.LocationPreconditionNotSatisfied))
            Mockito.`when`(settingsRepository.exposureNotificationsPausedState())
                .thenReturn(flowOf(Settings.PausedState.Enabled))
            Mockito.`when`(exposureNotificationsRepository.lastKeyProcessed())
                .thenReturn(flowOf(clock.millis()))
            Mockito.`when`(exposureNotificationsRepository.notificationsEnabledTimestamp())
                .thenReturn(flowOf(clock.millis()))
            Mockito.`when`(exposureNotificationsRepository.getLastExposureDate())
                .thenReturn(flowOf(null))
            Mockito.`when`(exposureNotificationsRepository.getLastNotificationReceivedDate())
                .thenReturn(null)
            Mockito.`when`(exposureNotificationsRepository.keyProcessingOverdue())
                .thenReturn(true)
            Mockito.`when`(notificationsRepository.exposureNotificationsEnabled())
                .thenReturn(flowOf(true))

            val statusViewModel = StatusViewModel(
                onboardingRepository,
                exposureNotificationsRepository,
                notificationsRepository,
                dashboardRepository,
                settingsRepository,
                appConfigManager,
                clock
            )

            statusViewModel.headerState.observeForTesting {
                Assert.assertEquals(
                    StatusViewModel.HeaderState.Disabled,
                    it.values.first()
                )
            }

            statusViewModel.notificationState.observeForTesting {
                Assert.assertEquals(
                    emptyList<StatusViewModel.NotificationState>(),
                    it.values.first()
                )
            }
        }

    @Test
    fun `resetErrorState calls rescheduleBackgroundJobs and resetNotificationsEnabledTimestamp`() {
        runBlocking {
            val clock = Clock.fixed(Instant.parse("2020-06-20T10:15:30.00Z"), ZoneId.of("UTC"))

            Mockito.`when`(exposureNotificationsRepository.getStatus())
                .thenReturn(flowOf(StatusResult.Enabled))
            Mockito.`when`(settingsRepository.exposureNotificationsPausedState())
                .thenReturn(flowOf(Settings.PausedState.Enabled))
            Mockito.`when`(exposureNotificationsRepository.lastKeyProcessed())
                .thenReturn(flowOf(clock.millis()))
            Mockito.`when`(exposureNotificationsRepository.notificationsEnabledTimestamp())
                .thenReturn(flowOf(clock.millis()))
            Mockito.`when`(exposureNotificationsRepository.getLastExposureDate())
                .thenReturn(flowOf(null))
            Mockito.`when`(exposureNotificationsRepository.getLastNotificationReceivedDate())
                .thenReturn(null)
            Mockito.`when`(exposureNotificationsRepository.keyProcessingOverdue())
                .thenReturn(false)
            Mockito.`when`(notificationsRepository.exposureNotificationsEnabled())
                .thenReturn(flowOf(true))

            val statusViewModel = StatusViewModel(
                onboardingRepository,
                exposureNotificationsRepository,
                notificationsRepository,
                dashboardRepository,
                settingsRepository,
                appConfigManager,
                clock
            )

            statusViewModel.resetErrorState()

            verify(exposureNotificationsRepository, times(1)).resetNotificationsEnabledTimestamp()
            verify(exposureNotificationsRepository, times(1)).rescheduleBackgroundJobs()
        }
    }

    @Test
    fun `dashboardState returns DashboardCards`() {
        runBlocking {
            val clock = Clock.fixed(Instant.parse("2020-06-20T10:15:30.00Z"), ZoneId.of("UTC"))

            Mockito.`when`(exposureNotificationsRepository.getStatus())
                .thenReturn(flowOf(StatusResult.Enabled))
            Mockito.`when`(settingsRepository.exposureNotificationsPausedState())
                .thenReturn(flowOf(Settings.PausedState.Enabled))
            Mockito.`when`(settingsRepository.dashboardEnabled)
                .thenReturn(true)
            Mockito.`when`(settingsRepository.getDashboardEnabledFlow())
                .thenReturn(flowOf(true))
            Mockito.`when`(exposureNotificationsRepository.lastKeyProcessed())
                .thenReturn(flowOf(clock.millis()))
            Mockito.`when`(exposureNotificationsRepository.notificationsEnabledTimestamp())
                .thenReturn(flowOf(clock.millis()))
            Mockito.`when`(exposureNotificationsRepository.getLastExposureDate())
                .thenReturn(flowOf(null))
            Mockito.`when`(exposureNotificationsRepository.getLastNotificationReceivedDate())
                .thenReturn(null)
            Mockito.`when`(exposureNotificationsRepository.keyProcessingOverdue())
                .thenReturn(false)
            Mockito.`when`(notificationsRepository.exposureNotificationsEnabled())
                .thenReturn(flowOf(true))
            Mockito.`when`(dashboardRepository.getDashboardData())
                .thenReturn(flowOf(Resource.Success(dashboardTestData)))

            val statusViewModel = StatusViewModel(
                onboardingRepository,
                exposureNotificationsRepository,
                notificationsRepository,
                dashboardRepository,
                settingsRepository,
                appConfigManager,
                clock
            )

            statusViewModel.refreshDashboardData()

            statusViewModel.dashboardState.observeForTesting {
                Assert.assertTrue(
                    it.values.first() is StatusViewModel.DashboardState.DashboardCards
                )
            }
        }
    }

    @Test
    fun `dashboardState returns ShowAsAction when framework is disabled`() = runBlocking {
        Mockito.`when`(exposureNotificationsRepository.getStatus())
            .thenReturn(flowOf(StatusResult.Disabled))
        Mockito.`when`(settingsRepository.exposureNotificationsPausedState())
            .thenReturn(flowOf(Settings.PausedState.Enabled))
        Mockito.`when`(settingsRepository.dashboardEnabled)
            .thenReturn(true)
        Mockito.`when`(settingsRepository.getDashboardEnabledFlow())
            .thenReturn(flowOf(true))
        Mockito.`when`(exposureNotificationsRepository.lastKeyProcessed())
            .thenReturn(flowOf(clock.millis()))
        Mockito.`when`(exposureNotificationsRepository.notificationsEnabledTimestamp())
            .thenReturn(flowOf(clock.millis()))
        Mockito.`when`(exposureNotificationsRepository.getLastExposureDate())
            .thenReturn(flowOf(null))
        Mockito.`when`(exposureNotificationsRepository.getLastNotificationReceivedDate())
            .thenReturn(null)
        Mockito.`when`(exposureNotificationsRepository.keyProcessingOverdue())
            .thenReturn(false)
        Mockito.`when`(notificationsRepository.exposureNotificationsEnabled())
            .thenReturn(flowOf(true))
        Mockito.`when`(dashboardRepository.getDashboardData())
            .thenReturn(flowOf(Resource.Success(dashboardTestData)))

        val statusViewModel = StatusViewModel(
            onboardingRepository,
            exposureNotificationsRepository,
            notificationsRepository,
            dashboardRepository,
            settingsRepository,
            appConfigManager,
            clock
        )

        statusViewModel.refreshDashboardData()

        statusViewModel.dashboardState.observeForTesting {
            Assert.assertTrue(
                it.values.first() is StatusViewModel.DashboardState.ShowAsAction
            )
        }
    }
}
