/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.model.DashboardData
import nl.rijksoverheid.en.api.model.FeatureFlagOption
import nl.rijksoverheid.en.config.AppConfigManager
import nl.rijksoverheid.en.dashboard.DashboardRepository
import nl.rijksoverheid.en.enapi.StatusResult
import nl.rijksoverheid.en.notifier.NotificationsRepository
import nl.rijksoverheid.en.onboarding.OnboardingRepository
import nl.rijksoverheid.en.settings.Settings
import nl.rijksoverheid.en.settings.SettingsRepository
import nl.rijksoverheid.en.util.Resource
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class StatusViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val exposureNotificationsRepository: ExposureNotificationsRepository,
    private val notificationsRepository: NotificationsRepository,
    private val dashboardRepository: DashboardRepository,
    settingsRepository: SettingsRepository,
    private val appConfigManager: AppConfigManager,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    fun isPlayServicesUpToDate() = onboardingRepository.isGooglePlayServicesUpToDate()

    val isIgnoringBatteryOptimizations: MutableStateFlow<Boolean> = MutableStateFlow(true)

    val headerState = combine(
        exposureNotificationsRepository.getStatus(),
        settingsRepository.exposureNotificationsPausedState(),
        exposureNotificationsRepository.lastKeyProcessed(),
        exposureNotificationsRepository.notificationsEnabledTimestamp()
    ) { status, pausedState, _, _ ->
        status to pausedState
    }.flatMapLatest { (status, pausedState) ->
        exposureNotificationsRepository.getLastExposureDate()
            .map { date -> Triple(status, pausedState, date) }
    }.map { (status, pausedState, exposedDate) ->
        createHeaderState(
            status,
            exposedDate,
            exposureNotificationsRepository.getLastNotificationReceivedDate(),
            exposureNotificationsRepository.keyProcessingOverdue(),
            settingsRepository.wifiOnly,
            pausedState
        )
    }.onEach {
        notificationsRepository.cancelExposureNotification()
    }.asLiveData(viewModelScope.coroutineContext)

    val exposureDetected: Boolean
        get() = headerState.value is HeaderState.Exposed

    val notificationState: LiveData<List<NotificationState>> = combine(
        exposureNotificationsRepository.notificationsEnabledTimestamp()
            .flatMapLatest { exposureNotificationsRepository.getStatus() },
        settingsRepository.exposureNotificationsPausedState(),
        exposureNotificationsRepository.getLastExposureDate(),
        notificationsRepository.exposureNotificationsEnabled(),
        isIgnoringBatteryOptimizations
    ) { statusResult, pausedState, lastExposureDate, exposureNotificationsEnabled, isIgnoringBatteryOptimizations ->
        getNotificationStates(
            lastExposureDate,
            exposureNotificationsRepository.getLastNotificationReceivedDate(),
            pausedState,
            isIgnoringBatteryOptimizations,
            suspend {
                getErrorState(
                    statusResult,
                    exposureNotificationsEnabled,
                    exposureNotificationsRepository.keyProcessingOverdue(),
                    settingsRepository.wifiOnly
                )
            }
        )
    }.asLiveData(viewModelScope.coroutineContext)

    val lastKeysProcessed = exposureNotificationsRepository.lastKeyProcessed()
        .map {
            if (it != null && it > 0)
                LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
            else
                null
        }.asLiveData(viewModelScope.coroutineContext)

    val exposureNotificationApiUpdateRequired = liveData {
        emit(exposureNotificationsRepository.isExposureNotificationApiUpdateRequired())
    }

    private val dashboardDataFlow: MutableStateFlow<Resource<DashboardData>> = MutableStateFlow(Resource.Loading())
    val dashboardData: LiveData<Resource<DashboardData>> = dashboardDataFlow.asLiveData(viewModelScope.coroutineContext)

    suspend fun getAppointmentInfo(context: Context): AppointmentInfo {
        val appConfig = appConfigManager.getCachedConfigOrDefault()
        val phoneNumber = if (exposureDetected)
            appConfig.appointmentPhoneNumber
        else
            context.getString(R.string.request_test_phone_number)
        return AppointmentInfo(
            phoneNumber = phoneNumber,
            website = appConfig.coronaTestURL
        )
    }

    suspend fun hasIndependentKeySharing() =
        appConfigManager.getCachedConfigOrDefault().hasFeature(FeatureFlagOption.INDEPENDENT_KEY_SHARING)

    fun hasCompletedOnboarding(): Boolean {
        return onboardingRepository.hasCompletedOnboarding()
    }

    private fun createHeaderState(
        status: StatusResult,
        lastExposureDate: LocalDate?,
        notificationReceivedDate: LocalDate?,
        keyProcessingOverdue: Boolean,
        isWifiOnlyOn: Boolean,
        pausedState: Settings.PausedState
    ): HeaderState {
        val exposureInLast14Days =
            lastExposureDate?.isAfter(LocalDate.now(clock).minusDays(15)) == true
        return when {
            lastExposureDate != null && exposureInLast14Days -> HeaderState.Exposed(
                lastExposureDate,
                notificationReceivedDate,
                clock
            )
            pausedState is Settings.PausedState.Paused -> HeaderState.Paused(pausedState.pausedUntil)
            status is StatusResult.Disabled -> HeaderState.Disabled
            status is StatusResult.LocationPreconditionNotSatisfied -> {
                if (keyProcessingOverdue) HeaderState.Disabled else HeaderState.LocationDisabled
            }
            status is StatusResult.BluetoothDisabled -> {
                if (keyProcessingOverdue) HeaderState.Disabled else HeaderState.BluetoothDisabled
            }
            keyProcessingOverdue && isWifiOnlyOn -> HeaderState.SyncIssuesWifiOnly
            keyProcessingOverdue && !isWifiOnlyOn -> HeaderState.SyncIssues
            status !is StatusResult.Enabled -> HeaderState.Disabled
            else -> HeaderState.Active
        }
    }

    private suspend fun getNotificationStates(
        lastExposureDate: LocalDate?,
        notificationReceivedDate: LocalDate?,
        pausedState: Settings.PausedState,
        isIgnoringBatteryOptimizations: Boolean,
        getErrorState: suspend () -> NotificationState.Error?
    ): List<NotificationState> {
        val notificationStates = mutableListOf<NotificationState>()
        val errorState = getErrorState()

        when {
            errorState is NotificationState.Error.NotificationsDisabled -> errorState
            lastExposureDate == null -> null
            lastExposureDate.isBefore(
                LocalDate.now(clock).minusDays(14)
            ) ->
                NotificationState.ExposureOver14DaysAgo(
                    lastExposureDate,
                    notificationReceivedDate,
                    clock
                )
            pausedState is Settings.PausedState.Paused ->
                NotificationState.Paused(pausedState.pausedUntil)
            else -> errorState
        }?.let { notificationStates.add(it) }

        // Add ErrorState.BatteryOptimizationEnabled independently from other error states if needed
        if (!isIgnoringBatteryOptimizations) notificationStates.add(NotificationState.BatteryOptimizationEnabled)
        return notificationStates
    }

    private fun getErrorState(
        status: StatusResult,
        exposureNotificationsEnabled: Boolean,
        keyProcessingOverdue: Boolean,
        isWifiOnlyOn: Boolean
    ): NotificationState.Error? = when (status) {
        StatusResult.Disabled,
        is StatusResult.Unavailable,
        is StatusResult.UnknownError -> NotificationState.Error.ConsentRequired
        StatusResult.BluetoothDisabled -> {
            if (keyProcessingOverdue)
                NotificationState.Error.ConsentRequired
            else NotificationState.Error.BluetoothDisabled
        }
        StatusResult.LocationPreconditionNotSatisfied -> {
            if (keyProcessingOverdue)
                NotificationState.Error.ConsentRequired
            else NotificationState.Error.LocationDisabled
        }
        StatusResult.Enabled -> {
            when {
                !exposureNotificationsEnabled -> NotificationState.Error.NotificationsDisabled
                keyProcessingOverdue && isWifiOnlyOn -> NotificationState.Error.SyncIssuesWifiOnly
                keyProcessingOverdue && !isWifiOnlyOn -> NotificationState.Error.SyncIssues
                else -> null
            }
        }
    }

    fun removeExposure() {
        viewModelScope.launch {
            exposureNotificationsRepository.resetExposures()
        }
    }

    fun resetErrorState() {
        viewModelScope.launch {
            exposureNotificationsRepository.resetNotificationsEnabledTimestamp()
            exposureNotificationsRepository.rescheduleBackgroundJobs()
        }
    }

    fun updateDashboardData() {
        viewModelScope.launch {
            dashboardRepository.getDashboardData().collect {
                // Ignore Loading state when we already have data to show
                if (dashboardData.value is Resource.Success && it is Resource.Loading)
                    return@collect

                dashboardDataFlow.emit(it)
            }
        }
    }

    sealed class HeaderState {
        object Active : HeaderState()
        object BluetoothDisabled : HeaderState()
        object LocationDisabled : HeaderState()
        object Disabled : HeaderState()
        object SyncIssues : HeaderState()
        object SyncIssuesWifiOnly : HeaderState()
        data class Paused(
            val pausedUntil: LocalDateTime
        ) : HeaderState()

        data class Exposed(
            val date: LocalDate,
            val notificationReceivedDate: LocalDate?,
            val clock: Clock
        ) : HeaderState()
    }

    sealed class NotificationState {

        data class Paused(val pausedUntil: LocalDateTime) : NotificationState()

        data class ExposureOver14DaysAgo(
            val exposureDate: LocalDate,
            val notificationReceivedDate: LocalDate?,
            val clock: Clock
        ) : NotificationState()

        object BatteryOptimizationEnabled : NotificationState()

        sealed class Error : NotificationState() {
            object BluetoothDisabled : Error()
            object LocationDisabled : Error()
            object ConsentRequired : Error()
            object NotificationsDisabled : Error()
            object SyncIssues : Error()
            object SyncIssuesWifiOnly : Error()
        }
    }

    class AppointmentInfo(val phoneNumber: String, val website: String)
}
