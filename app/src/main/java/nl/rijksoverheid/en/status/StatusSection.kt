/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import com.xwray.groupie.Item
import com.xwray.groupie.Section
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.model.DashboardData
import nl.rijksoverheid.en.api.model.DashboardItem
import nl.rijksoverheid.en.items.HorizontalRecyclerViewItem
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class StatusSection : Section() {

    private var headerState: StatusViewModel.HeaderState? = null
    private var notificationStates: List<StatusViewModel.NotificationState> = emptyList()

    private val headerGroup = Section()
    private val notificationGroup = Section().apply {
        setHideWhenEmpty(true)
    }
    private val notificationItems = mutableListOf<Item<*>>()

    private var dashboardData: DashboardData? = null
    private val dashboardGroup = Section().apply {
        setHideWhenEmpty(true)
    }

    var lastKeysProcessed: LocalDateTime? = null
        set(value) {
            field = value
            setFooter(StatusFooterItem(value))
        }

    init {
        setPlaceholder(LoadingItem())
    }

    fun refreshStateContent() {
        headerGroup.notifyChanged()
        notificationGroup.notifyChanged()
    }

    fun updateNotifications(
        notificationStates: List<StatusViewModel.NotificationState>,
        onAction: (StatusViewModel.NotificationState, NotificationAction) -> Unit,
    ) {
        if (this.notificationStates != notificationStates) {
            this.notificationStates = notificationStates
            notificationItems.clear()
            notificationStates.forEach {
                when (it) {
                    is StatusViewModel.NotificationState.Error -> {
                        notificationItems.add(
                            StatusErrorItem(it) {
                                onAction(it, NotificationAction.Primary)
                            }
                        )
                    }
                    is StatusViewModel.NotificationState.ExposureOver14DaysAgo -> {
                        notificationItems.add(
                            StatusExposureOver14DaysAgoItem.forStatus(it, onAction)
                        )
                    }
                    is StatusViewModel.NotificationState.Paused -> {
                        notificationItems.add(StatusPausedItem.forStatus(it, onAction))
                    }
                    is StatusViewModel.NotificationState.BatteryOptimizationEnabled ->
                        notificationItems.add(
                            StatusBatteryOptimizationEnabledItem.forStatus(it, onAction)
                        )
                }
            }
            notificationGroup.update(notificationItems)
        }
        ensureInitialized()
    }

    fun updateHeader(
        headerState: StatusViewModel.HeaderState,
        primaryAction: () -> Unit = {},
        secondaryAction: () -> Unit = {}
    ) {
        if (this.headerState != headerState) {
            this.headerState = headerState
            headerGroup.update(
                listOf(
                    StatusHeaderItem(
                        headerState,
                        primaryAction,
                        secondaryAction
                    )
                )
            )
        }
        ensureInitialized()
    }

   fun updateDashboardData(
       dashboardData: DashboardData,
       onItemClick: (DashboardItem) -> Unit
   ) {
       if (this.dashboardData != dashboardData) {
           this.dashboardData = dashboardData
           val dashboardItems = dashboardData.items
               .sortedBy { it.sortingValue }
               .map { StatusDashboardItem(it) }

           dashboardGroup.update(
               listOf(
                   HorizontalRecyclerViewItem(dashboardItems) { item, _ ->
                       val dashBoardItem = (item as? StatusDashboardItem)?.viewState?.dashboardItem
                       if (dashBoardItem != null)
                           onItemClick(dashBoardItem)
                   }
               )
           )
       }
       ensureInitialized()
    }

    private fun ensureInitialized() {
        if (isEmpty) {
            addAll(
                listOf(
                    headerGroup,
                    notificationGroup,
                    dashboardGroup,
                    Section(
                        listOf(
                            StatusActionItem.About,
                            StatusActionItem.Settings,
                            StatusActionItem.GenericNotification,
                            StatusActionItem.RequestTest,
                            StatusActionItem.Share,
                            StatusActionItem.LabTest
                        )
                    )
                )
            )
            setFooter(StatusFooterItem(lastKeysProcessed))
        }
    }

    enum class NotificationAction {
        Primary,
        Secondary
    }
}
