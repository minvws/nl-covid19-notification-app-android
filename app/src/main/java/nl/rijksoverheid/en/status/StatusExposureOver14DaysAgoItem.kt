/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import android.content.Context
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemStatusExposureOver14DaysAgoBinding
import nl.rijksoverheid.en.items.BaseBindableItem
import nl.rijksoverheid.en.util.formatDaysSince
import nl.rijksoverheid.en.util.formatExposureDate
import java.time.Clock
import java.time.LocalDate

class StatusExposureOver14DaysAgoItem(
    val viewState: ViewState
) : BaseBindableItem<ItemStatusExposureOver14DaysAgoBinding>() {

    data class ViewState(
        val exposureDate: LocalDate,
        val clock: Clock,
        val primaryAction: () -> Unit,
        val secondaryAction: () -> Unit
    ) {
        fun getMessage(context: Context) = context.getString(
            R.string.status_old_exposure_card_message,
            exposureDate.formatExposureDate(context),
            exposureDate.formatDaysSince(context, clock)
        )
    }

    override fun bind(viewBinding: ItemStatusExposureOver14DaysAgoBinding, position: Int) {
        viewBinding.viewState = viewState
    }

    override fun getLayout() = R.layout.item_status_exposure_over_14_days_ago

    companion object {
        fun forStatus(
            state: StatusViewModel.NotificationState.ExposureOver14DaysAgo,
            onAction: (StatusViewModel.NotificationState, StatusSection.NotificationAction) -> Unit
        ): StatusExposureOver14DaysAgoItem =
            StatusExposureOver14DaysAgoItem(
                ViewState(
                    state.exposureDate,
                    state.clock,
                    { onAction(state, StatusSection.NotificationAction.Primary) },
                    { onAction(state, StatusSection.NotificationAction.Secondary) }
                )
            )
    }
}
