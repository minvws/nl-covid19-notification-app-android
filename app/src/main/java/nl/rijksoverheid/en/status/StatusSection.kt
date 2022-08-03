/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import com.xwray.groupie.Item
import com.xwray.groupie.Section
import java.time.LocalDateTime

class StatusSection : Section() {

    private var headerState: StatusViewModel.HeaderState? = null
    private var notificationStates: List<StatusViewModel.NotificationState> = emptyList()

    private val headerGroup = Section()
    private val notificationGroup = Section().apply {
        setHideWhenEmpty(true)
    }
    private val notificationItems = mutableListOf<Item<*>>()

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
        onAction: (StatusViewModel.NotificationState, NotificationAction) -> Unit
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

    private fun ensureInitialized() {
        if (isEmpty) {
            addAll(
                listOf(
                    headerGroup,
                    notificationGroup,
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
