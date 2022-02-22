/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.status

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.model.DashboardItem
import nl.rijksoverheid.en.api.model.GraphValue
import nl.rijksoverheid.en.databinding.ItemStatusDashboardItemBinding
import nl.rijksoverheid.en.items.BaseBindableItem
import nl.rijksoverheid.en.util.formatExposureDateShort
import nl.rijksoverheid.en.util.formatToString
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

open class StatusDashboardItem(
    dashboardItem: DashboardItem
) : BaseBindableItem<ItemStatusDashboardItemBinding>() {

    private val viewState: ViewState = when(dashboardItem) {
        is DashboardItem.PositiveTestResults -> ViewState(
            R.drawable.ic_positive_test_results,
            R.string.status_dashboard_card_positive_test_results,
            dashboardItem.highlightedValue
        )
        is DashboardItem.CoronaMelderUsers -> ViewState(
            R.drawable.ic_corona_melder,
            R.string.status_dashboard_card_corona_melder_users,
            dashboardItem.highlightedValue
        )
        is DashboardItem.HospitalAdmissions -> ViewState(
            R.drawable.ic_hospital_admissions,
            R.string.status_dashboard_card_hospital_admissions,
            dashboardItem.highlightedValue
        )
        is DashboardItem.IcuAdmissions -> ViewState(
            R.drawable.ic_icu_admissions,
            R.string.status_dashboard_card_icu_admissions,
            dashboardItem.highlightedValue
        )
        is DashboardItem.VaccinationCoverage -> ViewState(
            R.drawable.ic_vaccination_coverage,
            R.string.status_dashboard_card_vaccination_coverage,
            dashboardItem.highlightedValue
        )
    }

    data class ViewState(
        @DrawableRes val icon: Int,
        @StringRes val title: Int,
        private val highlightedValue: GraphValue?,
        val clock: Clock = Clock.systemDefaultZone(),
    ) {
        private fun getLocalDateByTimestamp(timestamp: Long): LocalDate {
            return Instant.ofEpochMilli(timestamp)
                .atOffset(ZoneOffset.UTC)
                .toLocalDate()
        }

        fun getHighlightedLabel(context: Context) = highlightedValue?.timestamp
            ?.let { getLocalDateByTimestamp(it) }?.formatExposureDateShort(context)

        fun getHighlightedValue(context: Context) = highlightedValue?.value?.formatToString(context)
    }

    override fun getLayout() = R.layout.item_status_dashboard_item
    override fun isClickable() = true

    override fun bind(viewBinding: ItemStatusDashboardItemBinding, position: Int) {
        viewBinding.viewState = viewState
    }
}