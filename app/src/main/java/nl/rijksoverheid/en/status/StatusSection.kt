/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.status

import com.xwray.groupie.Section

class StatusSection : Section() {

    private val headerGroup = Section()
    private val errorGroup = Section()

    init {
        add(headerGroup)
        add(errorGroup)
        add(
            Section(
                listOf(
                    StatusActionItem.About,
                    StatusActionItem.GenericNotification,
                    StatusActionItem.RequestTest,
                    StatusActionItem.LabTest
                )
            )
        )
        setFooter(StatusFooterItem())
    }

    fun updateErrorState(errorState: StatusViewModel.ErrorState) {
        if (errorState is StatusViewModel.ErrorState.None) {
            errorGroup.clear()
        } else {
            errorGroup.update(listOf(StatusErrorItem(errorState)))
        }
    }

    fun updateHeader(headerState: StatusViewModel.HeaderState) {
        headerGroup.update(listOf(StatusHeaderItem(headerState)))
    }
}