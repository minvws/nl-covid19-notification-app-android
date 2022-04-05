/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.dashboard

import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.model.DashboardItem
import nl.rijksoverheid.en.databinding.ItemDashboardGraphBinding
import nl.rijksoverheid.en.items.BaseBindableItem
import nl.rijksoverheid.en.util.ext.applyDashboardStyling
import nl.rijksoverheid.en.util.ext.getIconTint
import nl.rijksoverheid.en.util.ext.icon
import nl.rijksoverheid.en.util.ext.title
import nl.rijksoverheid.en.util.formatToString

class DashboardGraphItem(
    private val dashboardItem: DashboardItem
) : BaseBindableItem<ItemDashboardGraphBinding>() {
    override fun getLayout() = R.layout.item_dashboard_graph

    override fun bind(viewBinding: ItemDashboardGraphBinding, position: Int) {
        val context = viewBinding.root.context

        viewBinding.dashboardItemTitle.setText(dashboardItem.title)
        viewBinding.dashboardItemTitle.setCompoundDrawablesWithIntrinsicBounds(dashboardItem.icon, 0, 0, 0)
        dashboardItem.getIconTint(context)?.let {
            viewBinding.dashboardItemTitle.compoundDrawables[0]?.setTint(it)
        }

        viewBinding.lineChart.apply {
            if (dashboardItem.values.isEmpty())
                return

            val entries = dashboardItem.values
                .map { Entry(it.timestamp.toFloat(), it.value.toFloat()) }
                .sortedBy { it.x }
            val dataSet = LineDataSet(entries, "")
            val maxValue = dashboardItem.values.maxOf { it.value }.toFloat()
            data = LineData(dataSet)

            accessibilityGraphDescription = context.getString(dashboardItem.title)

            val markerLabel = context.getString(dashboardItem.markerLabelRes)
            applyDashboardStyling(context, dataSet, maxValue, markerLabel) {
                it.toInt().formatToString(context)
            }
        }
    }

    private val DashboardItem.markerLabelRes: Int get() = when (this) {
        is DashboardItem.PositiveTestResults -> R.string.dashboard_positive_test_results_graph_marker
        is DashboardItem.CoronaMelderUsers -> R.string.dashboard_corona_melder_users_graph_marker
        is DashboardItem.HospitalAdmissions -> R.string.dashboard_hospital_admissions_graph_marker
        is DashboardItem.IcuAdmissions -> R.string.dashboard_icu_admissions_graph_marker
        is DashboardItem.VaccinationCoverage -> R.string.dashboard_vaccination_coverage_graph_marker
    }

    override fun isSameAs(other: Item<*>): Boolean =
        other is DashboardGraphItem

    override fun hasSameContentAs(other: Item<*>) =
        other is DashboardGraphItem && other.dashboardItem == dashboardItem
}
