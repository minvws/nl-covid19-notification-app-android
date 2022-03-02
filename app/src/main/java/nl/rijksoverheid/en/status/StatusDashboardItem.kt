/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.status

import android.content.Context
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.model.DashboardItem
import nl.rijksoverheid.en.databinding.ItemStatusDashboardItemBinding
import nl.rijksoverheid.en.databinding.ViewLabelledProgressBinding
import nl.rijksoverheid.en.items.BaseBindableItem
import nl.rijksoverheid.en.util.ext.applyCardViewStyling
import nl.rijksoverheid.en.util.ext.applyLineStyling
import nl.rijksoverheid.en.util.formatExposureDateShort
import nl.rijksoverheid.en.util.formatPercentageToString
import nl.rijksoverheid.en.util.formatToString
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

open class StatusDashboardItem(
    dashboardItem: DashboardItem
) : BaseBindableItem<ItemStatusDashboardItemBinding>() {

    val viewState: ViewState = ViewState(dashboardItem)

    data class ViewState(
        val dashboardItem: DashboardItem,
        val clock: Clock = Clock.systemDefaultZone(),
    ) {
        @DrawableRes val icon: Int = when (dashboardItem) {
            is DashboardItem.PositiveTestResults -> R.drawable.ic_positive_test_results
            is DashboardItem.CoronaMelderUsers -> R.drawable.ic_corona_melder
            is DashboardItem.HospitalAdmissions -> R.drawable.ic_hospital_admissions
            is DashboardItem.IcuAdmissions -> R.drawable.ic_icu_admissions
            is DashboardItem.VaccinationCoverage -> R.drawable.ic_vaccination_coverage
        }

        @StringRes val title: Int = when (dashboardItem) {
            is DashboardItem.PositiveTestResults -> R.string.status_dashboard_card_positive_test_results
            is DashboardItem.CoronaMelderUsers -> R.string.status_dashboard_card_corona_melder_users
            is DashboardItem.HospitalAdmissions -> R.string.status_dashboard_card_hospital_admissions
            is DashboardItem.IcuAdmissions -> R.string.status_dashboard_card_icu_admissions
            is DashboardItem.VaccinationCoverage -> R.string.status_dashboard_card_vaccination_coverage
        }

        private fun getLocalDateByTimestamp(timestamp: Long): LocalDate {
            return Instant.ofEpochMilli(timestamp)
                .atOffset(ZoneOffset.UTC)
                .toLocalDate()
        }

        fun getHighlightedLabel(context: Context) = dashboardItem.highlightedValue?.timestamp
            ?.let { getLocalDateByTimestamp(it) }?.formatExposureDateShort(context)

        fun getHighlightedValue(context: Context) = dashboardItem.highlightedValue?.value
            ?.formatToString(context)

        val cardStyle: CardStyle = when (dashboardItem) {
            is DashboardItem.PositiveTestResults -> CardStyle.GRAPH
            is DashboardItem.CoronaMelderUsers -> CardStyle.IMAGE
            is DashboardItem.HospitalAdmissions -> CardStyle.GRAPH
            is DashboardItem.IcuAdmissions -> CardStyle.GRAPH
            is DashboardItem.VaccinationCoverage -> CardStyle.PROGRESS
        }

        enum class CardStyle {
            GRAPH, IMAGE, PROGRESS
        }
    }

    override fun getLayout() = R.layout.item_status_dashboard_item
    override fun isClickable() = true

    override fun bind(viewBinding: ItemStatusDashboardItemBinding, position: Int) {
        viewBinding.viewState = viewState

        when (viewState.cardStyle) {
            ViewState.CardStyle.GRAPH -> bindGraphStyle(viewBinding)
            ViewState.CardStyle.IMAGE -> bindImageStyle(viewBinding)
            ViewState.CardStyle.PROGRESS -> bindProgressStyle(viewBinding)
        }
    }

    private fun bindGraphStyle(viewBinding: ItemStatusDashboardItemBinding) {
        viewBinding.image.isVisible = false
        viewBinding.progressContainer.isVisible = false

        if (viewState.dashboardItem.values.isNotEmpty()) {
            viewBinding.lineChart.apply {

                val entries = viewState.dashboardItem.values
                    .map { Entry(it.timestamp.toFloat(), it.value.toFloat()) }
                    .sortedBy { it.x }
                val dataSet = LineDataSet(entries, "")
                    .apply {
                        applyLineStyling(context)
                        getEntryForIndex(entries.size - 1).icon =
                            ContextCompat.getDrawable(context, R.drawable.ic_graph_dot_indicator)
                    }

                data = LineData(dataSet)

                applyCardViewStyling()

                val horizontalOffset = resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin).toFloat()
                setViewPortOffsets(horizontalOffset, 0f, horizontalOffset, 0f)
                isVisible = true

                //Fix: SetViewPortOffSets are not applied the first time
                post { invalidate() }
            }
        } else {
            viewBinding.lineChart.isVisible = false
        }
    }

    private fun bindImageStyle(viewBinding: ItemStatusDashboardItemBinding) {
        viewBinding.lineChart.isVisible = false
        viewBinding.image.isVisible = true
        viewBinding.progressContainer.isVisible = false

        if (viewState.dashboardItem is DashboardItem.CoronaMelderUsers) {
            viewBinding.image.setImageResource(R.drawable.ic_corona_melder_users)
        }
    }

    private fun bindProgressStyle(viewBinding: ItemStatusDashboardItemBinding) {
        viewBinding.lineChart.isVisible = false
        viewBinding.image.isVisible = false
        viewBinding.progressContainer.isVisible = true

        val context = viewBinding.root.context

        viewBinding.progressContainer.removeAllViewsInLayout()
        if (viewState.dashboardItem is DashboardItem.VaccinationCoverage) {
            val layoutInflater = LayoutInflater.from(context)

            ViewLabelledProgressBinding.inflate(layoutInflater, viewBinding.progressContainer, true).apply {
                descriptionText.setText(R.string.status_dashboard_card_vaccination_coverage_elder)
                percentageText.text = viewState.dashboardItem.elderCoverage.formatPercentageToString()
                progressIndicator.progress = viewState.dashboardItem.elderCoverage.toInt()
            }
            ViewLabelledProgressBinding.inflate(layoutInflater, viewBinding.progressContainer, true).apply {
                descriptionText.setText(R.string.status_dashboard_card_vaccination_coverage_booster)
                percentageText.text = viewState.dashboardItem.boosterCoverage.formatPercentageToString()
                progressIndicator.progress = viewState.dashboardItem.boosterCoverage.toInt()
            }
        }
    }

}