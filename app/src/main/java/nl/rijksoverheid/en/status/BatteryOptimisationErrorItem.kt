/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import android.content.Context
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemStatusErrorBinding

class BatteryOptimisationErrorItem(private val action: () -> Unit) : BaseStatusErrorItem() {

    override val viewState =
        object : ErrorViewState(R.string.status_error_action_disable_battery_optimisation, action) {
            override fun getMessage(context: Context) =
                context.getString(R.string.status_error_battery_optimisation)
        }

    override fun bind(viewBinding: ItemStatusErrorBinding, position: Int) {
        viewBinding.viewState = viewState
    }

    override fun getLayout() = R.layout.item_status_error
}
