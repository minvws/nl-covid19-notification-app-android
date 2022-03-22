/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.dashboard

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.model.DashboardItem
import nl.rijksoverheid.en.databinding.ItemDashboardLinkBinding
import nl.rijksoverheid.en.items.BaseBindableItem
import nl.rijksoverheid.en.util.DateTimeHelper
import nl.rijksoverheid.en.util.ext.getIconTint
import nl.rijksoverheid.en.util.ext.icon
import nl.rijksoverheid.en.util.ext.title
import nl.rijksoverheid.en.util.formatDashboardDateShort
import nl.rijksoverheid.en.util.formatPercentageToString
import nl.rijksoverheid.en.util.formatToString
import java.time.Clock

class DashboardLinkItem(
    private val dashboardItem: DashboardItem,
    private val onDashboardLinkItemClicked: (DashboardItem.Reference) -> Unit
) : BaseBindableItem<ItemDashboardLinkBinding>() {
    override fun getLayout() = R.layout.item_dashboard_link

    val viewState = ViewState(dashboardItem)

    data class ViewState(
        val dashboardItem: DashboardItem,
        val clock: Clock = Clock.systemDefaultZone(),
    ) {
        @DrawableRes
        val icon: Int = dashboardItem.icon

        @ColorInt
        fun getTintColor(context: Context) = dashboardItem.getIconTint(context)

        @StringRes
        val title: Int = dashboardItem.title

        fun getSubtitle(context: Context): Spannable {
            val highlightedLabel = when (dashboardItem) {
                is DashboardItem.VaccinationCoverage ->
                    context
                        .getString(R.string.dashboard_vaccination_coverage_booster_label)
                else -> dashboardItem.highlightedValue?.let {
                    DateTimeHelper.convertToLocalDate(it.timestamp)
                        .formatDashboardDateShort(context)
                } ?: ""
            }

            val highlightedValue = when (dashboardItem) {
                is DashboardItem.VaccinationCoverage -> dashboardItem.boosterCoverage18Plus.formatPercentageToString()
                else -> dashboardItem.highlightedValue?.value?.formatToString(context) ?: ""
            }

            return SpannableStringBuilder()
                .append(highlightedLabel)
                .append(" ")
                .append(
                    highlightedValue,
                    StyleSpan(Typeface.BOLD),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
        }
    }

    override fun bind(viewBinding: ItemDashboardLinkBinding, position: Int) {
        viewBinding.viewState = viewState
        viewBinding.cardView.setOnClickListener {
            onDashboardLinkItemClicked(dashboardItem.reference)
        }
    }

    override fun isSameAs(other: Item<*>): Boolean =
        other is DashboardLinkItem && other.dashboardItem.reference == dashboardItem.reference

    override fun hasSameContentAs(other: Item<*>) =
        other is DashboardLinkItem && other.dashboardItem == dashboardItem
}
