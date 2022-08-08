/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.dashboard

import android.content.Context
import com.xwray.groupie.Section
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.model.DashboardData
import nl.rijksoverheid.en.items.DashboardCardItem
import nl.rijksoverheid.en.items.HeaderItem
import nl.rijksoverheid.en.items.LinkItem
import nl.rijksoverheid.en.items.ParagraphItem
import nl.rijksoverheid.en.items.TagItem
import nl.rijksoverheid.en.status.items.LoadingItem

class DashboardOverviewSection : Section() {

    init {
        setPlaceholder(LoadingItem())
    }

    fun updateDashboardData(
        context: Context,
        dashboardData: DashboardData,
        onMoreInfoLinkItemClicked: () -> Unit
    ) {
        val dashboardItems = dashboardData.items
            .sortedBy { it.sortingValue }
            .map { dashboardItem -> DashboardCardItem(context, dashboardItem) }

        update(
            listOf(
                HeaderItem(R.string.dashboard_header),
                TagItem(R.string.dashboard_tag),
                ParagraphItem(R.string.dashboard_overview_summary)
            ) +
                dashboardItems +
                LinkItem(R.string.dashboard_more_info_link, onClick = onMoreInfoLinkItemClicked)
        )
    }
}
