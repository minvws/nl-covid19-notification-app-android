/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.dashboard

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.model.DashboardItem
import nl.rijksoverheid.en.databinding.ItemDashboardLinkBinding
import nl.rijksoverheid.en.items.BaseBindableItem
import nl.rijksoverheid.en.util.DateTimeHelper
import nl.rijksoverheid.en.util.ext.getIconTint
import nl.rijksoverheid.en.util.ext.icon
import nl.rijksoverheid.en.util.ext.title
import nl.rijksoverheid.en.util.formatDateShort
import nl.rijksoverheid.en.util.formatExposureDateShort
import nl.rijksoverheid.en.util.formatToString
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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

        fun getHighlightedValue(context: Context) = dashboardItem.highlightedValue?.let{ StringBuilder()
            .append(DateTimeHelper.convertToLocalDate(it.timestamp).formatDateShort(context))
            .append(" ")
            .append(it.value.formatToString(context))
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