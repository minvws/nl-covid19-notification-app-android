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
import nl.rijksoverheid.en.api.model.DashboardItemRef
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

    private val dashboardItems = Section()

    init {
        setPlaceholder(LoadingItem())
        add(dashboardItems)
    }

    fun updateDashboardData(
        context: Context,
        selectedDashboardItem: DashboardItemRef,
        dashboardData: DashboardData,
        onDashboardLinkItemClicked: (DashboardItemRef) -> Unit,
        onMoreInfoLinkItemClicked: () -> Unit
    ) {
        val dashboardItem = dashboardData.getDashboardItem(selectedDashboardItem) ?: return
        val headerRes = when (selectedDashboardItem) {
            DashboardItemRef.PositiveTestResults -> R.string.dashboard_positive_test_results_header
            DashboardItemRef.CoronaMelderUsers -> R.string.dashboard_corona_melder_users_header
            DashboardItemRef.HospitalAdmissions -> R.string.dashboard_hospital_admissions_header
            DashboardItemRef.IcuAdmissions -> R.string.dashboard_icu_admissions_header
            DashboardItemRef.VaccinationCoverage -> R.string.dashboard_vaccination_coverage_header
        }
        val summaryRes = when (selectedDashboardItem) {
            DashboardItemRef.PositiveTestResults -> R.string.dashboard_positive_test_results_summary
            DashboardItemRef.CoronaMelderUsers -> R.string.dashboard_corona_melder_users_summary
            DashboardItemRef.HospitalAdmissions -> R.string.dashboard_hospital_admissions_summary
            DashboardItemRef.IcuAdmissions -> R.string.dashboard_icu_admissions_summary
            DashboardItemRef.VaccinationCoverage -> R.string.dashboard_vaccination_coverage_summary
        }

        val summaryArgs: List<String> = when (dashboardItem) {
            is DashboardItem.PositiveTestResults ->
                dashboardItem.infectedMovingAverage.toSummaryArgs(context) +
                    listOf(dashboardItem.confirmedCases.formatPercentageToString(context))
            is DashboardItem.CoronaMelderUsers -> dashboardItem.highlightedValue?.let {
                listOf(
                    DateTimeHelper.convertToLocalDate(it.timestamp).formatDashboardDateShort(context),
                    it.value.formatToString(context)
                )
            } ?: emptyList()
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

        dashboardItems.update(items)
    }

    private fun MovingAverage.toSummaryArgs(context: Context) = listOf(
        value.formatToString(context),
        DateTimeHelper.convertToLocalDate(timestampStart).formatDashboardDateShort(context),
        DateTimeHelper.convertToLocalDate(timestampEnd).formatDashboardDateShort(context)
    )
}
