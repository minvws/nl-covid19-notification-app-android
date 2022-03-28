/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import android.content.Context
import android.content.res.Configuration
import com.xwray.groupie.Item
import com.xwray.groupie.Section
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.model.DashboardItem
import nl.rijksoverheid.en.items.HorizontalRecyclerViewItem
import nl.rijksoverheid.en.status.items.LoadingItem
import nl.rijksoverheid.en.status.items.StatusActionDashboardItem
import nl.rijksoverheid.en.status.items.StatusActionItem
import nl.rijksoverheid.en.status.items.StatusBatteryOptimizationEnabledItem
import nl.rijksoverheid.en.status.items.StatusDashboardErrorItem
import nl.rijksoverheid.en.status.items.StatusDashboardHeaderItem
import nl.rijksoverheid.en.items.DashboardCardItem
import nl.rijksoverheid.en.status.items.StatusDashboardLoadingItem
import nl.rijksoverheid.en.status.items.StatusErrorItem
import nl.rijksoverheid.en.status.items.StatusExposureOver14DaysAgoItem
import nl.rijksoverheid.en.status.items.StatusFooterItem
import nl.rijksoverheid.en.status.items.StatusHeaderItem
import nl.rijksoverheid.en.status.items.StatusPausedItem
import nl.rijksoverheid.en.util.Resource
import java.time.LocalDateTime


class StatusSection : Section() {

    private var headerState: StatusViewModel.HeaderState? = null
    private var notificationStates: List<StatusViewModel.NotificationState> = emptyList()

    private val headerGroup = Section()
    private val notificationGroup = Section().apply {
        setHideWhenEmpty(true)
    }
    private val notificationItems = mutableListOf<Item<*>>()

    private var highestDashboardItemHeight = 0
    private var dashboardState: StatusViewModel.DashboardState? = null
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
        context: Context,
        dashboardState: StatusViewModel.DashboardState,
        onItemClick: (DashboardItem) -> Unit,
        highestItemHeight: Int = context.resources.getDimensionPixelSize(R.dimen.dashboard_content_min_height)
    ) {
        val dashboardData = dashboardState.resource
        val currentDashboardData = this.dashboardState?.resource

        val contentDidNotChange = (currentDashboardData is Resource.Loading && dashboardData is Resource.Loading) ||
            (currentDashboardData is Resource.Success && dashboardData is Resource.Success && currentDashboardData.data == dashboardData.data) ||
            (currentDashboardData is Resource.Error && dashboardData is Resource.Error && currentDashboardData.error == dashboardData.error)

        if (contentDidNotChange &&
            dashboardState.showAsAction == this.dashboardState?.showAsAction &&
            highestItemHeight <= highestDashboardItemHeight)
            return

        this.dashboardState = dashboardState
        this.highestDashboardItemHeight = highestItemHeight

        val items = when {
            dashboardState.showAsAction -> listOf(
                StatusActionDashboardItem
            )
            dashboardData is Resource.Loading -> listOf(
                StatusDashboardHeaderItem,
                StatusDashboardLoadingItem
            )
            dashboardData is Resource.Error -> listOf(
                StatusDashboardHeaderItem,
                StatusDashboardErrorItem(dashboardData.error.peekContent())
            )
            dashboardData is Resource.Success -> {

                val resources = context.resources
                val dashboardItemWidth = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    context.resources.displayMetrics.widthPixels - resources.getDimensionPixelSize(R.dimen.dashboard_content_width_margin)
                } else {
                    context.resources.displayMetrics.widthPixels / 2
                }

                val dashboardItems = dashboardData.data.items
                    .sortedBy { it.sortingValue }
                    .map { dashboardItem ->
                        DashboardCardItem(
                            context = context,
                            dashboardItem = dashboardItem,
                            contentWidth = dashboardItemWidth,
                            minHeight = highestItemHeight
                        ) { updatedHighestItemHeight ->
                            updateDashboardData(context, dashboardState, onItemClick, updatedHighestItemHeight)
                        }
                    }

                listOf(
                    StatusDashboardHeaderItem,
                    HorizontalRecyclerViewItem(dashboardItems) { item, _ ->
                        (item as? DashboardCardItem)?.dashboardItem?.let { onItemClick(it) }
                    }
                )
            }
            else -> emptyList()
        }

        dashboardGroup.update(items)
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
