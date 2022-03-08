/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.dashboard

import androidx.core.content.ContextCompat
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.model.DashboardItem
import nl.rijksoverheid.en.databinding.ItemDashboardGraphBinding
import nl.rijksoverheid.en.items.BaseBindableItem
import nl.rijksoverheid.en.util.ext.applyDashboardStyling
import nl.rijksoverheid.en.util.ext.icon
import nl.rijksoverheid.en.util.ext.title

class DashboardGraphItem(
    private val dashboardItem: DashboardItem
) : BaseBindableItem<ItemDashboardGraphBinding>() {
    override fun getLayout() = R.layout.item_dashboard_graph

    override fun bind(viewBinding: ItemDashboardGraphBinding, position: Int) {

        viewBinding.dashboardItemTitle.setCompoundDrawablesWithIntrinsicBounds(dashboardItem.icon, 0, 0, 0)
        viewBinding.dashboardItemTitle.setText(dashboardItem.title)

        viewBinding.lineChart.apply {
            val entries = dashboardItem.values
                .map { Entry(it.timestamp.toFloat(), it.value.toFloat()) }
                .sortedBy { it.x }
            val dataSet = LineDataSet(entries, "")
            data = LineData(dataSet)

            applyDashboardStyling(context, dataSet)

            //val horizontalOffset = resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin).toFloat()
            //setViewPortOffsets(horizontalOffset, 0f, horizontalOffset, 0f)

            //Fix: SetViewPortOffSets are not applied the first time
            //post { invalidate() }
        }
    }

    override fun isSameAs(other: Item<*>): Boolean =
        other is DashboardGraphItem

    override fun hasSameContentAs(other: Item<*>) =
        other is DashboardGraphItem && other.dashboardItem == dashboardItem
}