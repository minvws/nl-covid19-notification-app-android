/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import com.xwray.groupie.Section

class StatusSection(isInTestPhase: Boolean) : Section() {

    private var headerState: StatusViewModel.HeaderState? = null
    private var errorState: StatusViewModel.ErrorState = StatusViewModel.ErrorState.None

    private val headerGroup = Section()
    private val errorGroup = Section()

    init {
        addAll(
            listOf(
                headerGroup,
                errorGroup,
                Section(
                    listOf(
                        StatusActionItem.About,
                        StatusActionItem.Share,
                        StatusActionItem.GenericNotification,
                        StatusActionItem.RequestTest,
                        StatusActionItem.LabTest
                    )
                )
            )
        )
        setFooter(if (isInTestPhase) TestStatusFooterItem() else StatusFooterItem())
    }

    fun updateErrorState(
        errorState: StatusViewModel.ErrorState,
        action: () -> Unit = {}
    ) {
        if (this.errorState != errorState) {
            this.errorState = errorState
            if (errorState is StatusViewModel.ErrorState.None) {
                errorGroup.clear()
            } else {
                errorGroup.update(listOf(StatusErrorItem(errorState, action)))
            }
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
    }
}
