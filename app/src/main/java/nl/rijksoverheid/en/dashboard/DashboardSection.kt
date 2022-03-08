/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.dashboard

import com.xwray.groupie.Section
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.model.DashboardData
import nl.rijksoverheid.en.api.model.DashboardItem
import nl.rijksoverheid.en.items.HeaderItem
import nl.rijksoverheid.en.items.ParagraphItem

class DashboardSection: Section() {

    fun updateDashboardData(
        selectedDashboardItem: DashboardItem.Reference,
        dashboardData: DashboardData,
        onDashboardLinkItemClicked: (DashboardItem.Reference) -> Unit
    ) {
        val dashboardItem = dashboardData.getDashboardItem(selectedDashboardItem) ?: return
        val headerRes = when (selectedDashboardItem) {
            DashboardItem.Reference.PositiveTestResults -> R.string.dashboard_positive_test_results_header
            DashboardItem.Reference.CoronaMelderUsers -> R.string.dashboard_corona_melder_users_header
            DashboardItem.Reference.HospitalAdmissions -> R.string.dashboard_hospital_admissions_header
            DashboardItem.Reference.IcuAdmissions -> R.string.dashboard_icu_admissions_header
            DashboardItem.Reference.VaccinationCoverage -> R.string.dashboard_vaccination_coverage_header
        }
        val summaryRes = when (selectedDashboardItem) {
            DashboardItem.Reference.PositiveTestResults -> R.string.dashboard_positive_test_results_summary
            DashboardItem.Reference.CoronaMelderUsers -> R.string.dashboard_corona_melder_users_summary
            DashboardItem.Reference.HospitalAdmissions -> R.string.dashboard_hospital_admissions_summary
            DashboardItem.Reference.IcuAdmissions -> R.string.dashboard_icu_admissions_summary
            DashboardItem.Reference.VaccinationCoverage -> R.string.dashboard_vaccination_coverage_summary
        }

        val items = listOf(
            HeaderItem(headerRes),
            ParagraphItem(summaryRes),
            DashboardGraphItem(dashboardItem),
            HeaderItem(R.string.dashboard_more_info_header)
        ) + dashboardData.items
            .filter { it.reference != selectedDashboardItem }
            .map { DashboardLinkItem(it, onDashboardLinkItemClicked) }

        update(items)
    }
}
