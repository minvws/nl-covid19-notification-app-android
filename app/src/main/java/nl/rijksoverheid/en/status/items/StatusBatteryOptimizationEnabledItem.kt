/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status.items

import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemStatusBatteryOptimizationEnabledBinding
import nl.rijksoverheid.en.items.BaseBindableItem
import nl.rijksoverheid.en.status.StatusSection
import nl.rijksoverheid.en.status.StatusViewModel

class StatusBatteryOptimizationEnabledItem(val viewState: ViewState) :
    BaseBindableItem<ItemStatusBatteryOptimizationEnabledBinding>() {

    data class ViewState(val action: () -> Unit)

    override fun bind(viewBinding: ItemStatusBatteryOptimizationEnabledBinding, position: Int) {
        viewBinding.viewState = viewState
    }

    override fun getLayout() = R.layout.item_status_battery_optimization_enabled

    companion object {
        fun forStatus(
            state: StatusViewModel.NotificationState.BatteryOptimizationEnabled,
            onAction: (StatusViewModel.NotificationState, StatusSection.NotificationAction) -> Unit
        ): StatusBatteryOptimizationEnabledItem =
            StatusBatteryOptimizationEnabledItem(
                ViewState { onAction(state, StatusSection.NotificationAction.Primary) }
            )
    }
}
