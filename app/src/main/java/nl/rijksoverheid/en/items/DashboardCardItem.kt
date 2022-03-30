/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.items

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.model.DashboardItem
import nl.rijksoverheid.en.databinding.ItemDashboardCardBinding
import nl.rijksoverheid.en.databinding.ViewLabelledProgressBinding
import nl.rijksoverheid.en.util.DateTimeHelper
import nl.rijksoverheid.en.util.ext.applyCardViewStyling
import nl.rijksoverheid.en.util.ext.applyLineStyling
import nl.rijksoverheid.en.util.ext.getIconTint
import nl.rijksoverheid.en.util.ext.icon
import nl.rijksoverheid.en.util.ext.title
import nl.rijksoverheid.en.util.formatDashboardDateShort
import nl.rijksoverheid.en.util.formatPercentageToString
import nl.rijksoverheid.en.util.formatToString

open class DashboardCardItem(
    context: Context,
    val dashboardItem: DashboardItem,
    private val contentWidth: Int = ViewGroup.LayoutParams.MATCH_PARENT,
    private val minHeight: Int = context.resources.getDimensionPixelSize(R.dimen.dashboard_content_min_height),
    private val setHighestHeight: ((Int) -> Unit)? = null,
) : BaseBindableItem<ItemDashboardCardBinding>() {

    override fun getLayout() = R.layout.item_dashboard_card
    override fun isClickable() = true

    override fun bind(viewBinding: ItemDashboardCardBinding, position: Int) {
        val context = viewBinding.root.context

        viewBinding.dashboardItemIcon.apply {
            setImageResource(dashboardItem.icon)
            val iconTint = dashboardItem.getIconTint(context)
            if (iconTint != null)
                setColorFilter(iconTint)
            else
                clearColorFilter()
        }
        viewBinding.dashboardItemTitle.setText(dashboardItem.title)
        viewBinding.highlightedLabel.text = dashboardItem.highlightedValue?.timestamp
            ?.let { DateTimeHelper.convertToLocalDate(it) }?.formatDashboardDateShort(context)
        viewBinding.highlightedValue.text = dashboardItem.highlightedValue?.value
            ?.formatToString(context)

        when (dashboardItem.cardStyle) {
            CardStyle.GRAPH -> bindGraphStyle(viewBinding)
            CardStyle.IMAGE -> bindImageStyle(viewBinding)
            CardStyle.PROGRESS -> bindProgressStyle(viewBinding)
        }

        viewBinding.container.layoutParams = viewBinding.container.layoutParams.apply {
            width = contentWidth
        }

        if (setHighestHeight != null) {
            viewBinding.container.minHeight = minHeight
            viewBinding.container.post {
                val measuredHeight = viewBinding.container.measuredHeight
                if (measuredHeight > minHeight)
                    setHighestHeight.invoke(measuredHeight)
            }
        }
    }

    private fun bindGraphStyle(viewBinding: ItemDashboardCardBinding) {
        viewBinding.image.isVisible = false
        viewBinding.progressContainer.isVisible = false

        if (dashboardItem.values.isNotEmpty()) {
            viewBinding.lineChart.apply {

                val entries = dashboardItem.values
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

                isVisible = true
            }
        } else {
            viewBinding.lineChart.isVisible = false
        }
    }

    private fun bindImageStyle(viewBinding: ItemDashboardCardBinding) {
        viewBinding.lineChart.isVisible = false
        viewBinding.image.isVisible = true
        viewBinding.progressContainer.isVisible = false

        if (dashboardItem is DashboardItem.CoronaMelderUsers) {
            viewBinding.image.setImageResource(R.drawable.ic_corona_melder_users)
        }
    }

    private fun bindProgressStyle(viewBinding: ItemDashboardCardBinding) {
        viewBinding.lineChart.isVisible = false
        viewBinding.image.isVisible = false
        viewBinding.progressContainer.isVisible = true

        val context = viewBinding.root.context

        viewBinding.progressContainer.removeAllViewsInLayout()
        if (dashboardItem is DashboardItem.VaccinationCoverage) {
            val layoutInflater = LayoutInflater.from(context)

            ViewLabelledProgressBinding.inflate(layoutInflater, viewBinding.progressContainer, true).apply {
                descriptionText.text = formatProgressLabel(
                    context,
                    dashboardItem.vaccinationCoverage18Plus,
                    R.string.dashboard_vaccination_coverage_elder_label
                )
                progressIndicator.progress = dashboardItem.vaccinationCoverage18Plus.toInt()
            }
            ViewLabelledProgressBinding.inflate(layoutInflater, viewBinding.progressContainer, true).apply {
                descriptionText.text = formatProgressLabel(
                    context,
                    dashboardItem.boosterCoverage18Plus,
                    R.string.dashboard_vaccination_coverage_booster_label
                )
                progressIndicator.progress = dashboardItem.boosterCoverage18Plus.toInt()
            }
        }
    }

    private fun formatProgressLabel(context: Context, percentage: Float, @StringRes description: Int): Spannable {
        val formattedPercentage = percentage.formatPercentageToString(context)
        return SpannableStringBuilder()
            .append(formattedPercentage, StyleSpan(Typeface.BOLD), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            .append(" ")
            .append(context.getString(description))
    }

    private val DashboardItem.cardStyle: CardStyle get() = when (this) {
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
