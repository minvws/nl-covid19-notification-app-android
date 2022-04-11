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
import nl.rijksoverheid.en.api.model.DashboardItem
import nl.rijksoverheid.en.api.model.MovingAverage
import nl.rijksoverheid.en.items.HeaderItem
import nl.rijksoverheid.en.items.LinkItem
import nl.rijksoverheid.en.items.ParagraphItem
import nl.rijksoverheid.en.status.items.LoadingItem
import nl.rijksoverheid.en.util.DateTimeHelper
import nl.rijksoverheid.en.util.formatDashboardDateShort
import nl.rijksoverheid.en.util.formatPercentageToString
import nl.rijksoverheid.en.util.formatToString

class DashboardSection : Section() {

    init {
        setPlaceholder(LoadingItem())
    }

    fun updateDashboardData(
        context: Context,
        selectedDashboardItem: DashboardItem.Reference,
        dashboardData: DashboardData,
        onDashboardLinkItemClicked: (DashboardItem.Reference) -> Unit,
        onMoreInfoLinkItemClicked: () -> Unit
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

        val summaryArgs: List<String> = when (dashboardItem) {
            is DashboardItem.PositiveTestResults ->
                dashboardItem.infectedMovingAverage.toSummaryArgs(context) +
                    listOf(dashboardItem.confirmedCases.formatPercentageToString(context))
            is DashboardItem.CoronaMelderUsers -> listOfNotNull(
                dashboardItem.highlightedValue?.value?.formatToString(context)
            )
            is DashboardItem.HospitalAdmissions ->
                dashboardItem.hospitalAdmissionMovingAverage.toSummaryArgs(context)
            is DashboardItem.IcuAdmissions -> dashboardItem.icuAdmissionMovingAverage.toSummaryArgs(context)
            is DashboardItem.VaccinationCoverage -> listOf(
                dashboardItem.vaccinationCoverage18Plus.formatPercentageToString(context),
                dashboardItem.boosterCoverage18Plus.formatPercentageToString(context)
            )
        }

        val items = listOf(
            HeaderItem(headerRes),
            ParagraphItem(summaryRes, *summaryArgs.toTypedArray()),
            DashboardGraphItem(dashboardItem),
            LinkItem(R.string.dashboard_more_info_link, onClick = onMoreInfoLinkItemClicked),
            HeaderItem(R.string.dashboard_more_info_header)
        ) + dashboardData.items
            .filter { it.reference != selectedDashboardItem }
            .map { DashboardLinkItem(it, onDashboardLinkItemClicked) }

        update(items)
    }

    private fun MovingAverage.toSummaryArgs(context: Context) = listOf(
        value.formatToString(context),
        DateTimeHelper.convertToLocalDate(timestampStart).formatDashboardDateShort(context),
        DateTimeHelper.convertToLocalDate(timestampEnd).formatDashboardDateShort(context),
    )
}
