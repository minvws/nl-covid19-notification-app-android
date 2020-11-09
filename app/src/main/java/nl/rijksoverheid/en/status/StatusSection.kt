/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import com.xwray.groupie.Item
import com.xwray.groupie.Section

class StatusSection : Section() {

    private var headerState: StatusViewModel.HeaderState? = null
    private var errorState: StatusViewModel.ErrorState = StatusViewModel.ErrorState.None

    private val headerGroup = Section()
    private val errorGroup = Section().apply {
        setHideWhenEmpty(true)
    }
    private val errorItems = mutableListOf<Item<*>>()

    init {
        setPlaceholder(LoadingItem())
    }

    fun updateErrorState(
        errorState: StatusViewModel.ErrorState,
        action: () -> Unit = {}
    ) {
        if (this.errorState != errorState) {
            this.errorState = errorState
            errorItems.removeAll { it is StatusErrorItem }
            if (errorState !is StatusViewModel.ErrorState.None) {
                errorItems.add(0, StatusErrorItem(errorState, action))
            }
            errorGroup.update(errorItems)
        }
        ensureInitialized()
    }

    fun showBatteryOptimisationsError(action: () -> Unit) {
        if (errorItems.firstOrNull { it is BatteryOptimisationErrorItem } == null) {
            errorItems.add(BatteryOptimisationErrorItem(action))
            errorGroup.update(errorItems)
        }
    }

    fun removeBatteryOptimisationsError() {
        if (errorItems.removeAll { it is BatteryOptimisationErrorItem }) {
            errorGroup.update(errorItems)
        }
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
                    headerGroup, errorGroup,
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
            setFooter(StatusFooterItem())
        }
    }
}
