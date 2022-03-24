/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
package nl.rijksoverheid.en.status.items

import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemStatusDashboardErrorBinding
import nl.rijksoverheid.en.items.BaseBindableItem
import nl.rijksoverheid.en.util.ErrorType

class StatusDashboardErrorItem(val error: ErrorType) :
    BaseBindableItem<ItemStatusDashboardErrorBinding>() {
    override fun getLayout() = R.layout.item_status_dashboard_error

    override fun bind(viewBinding: ItemStatusDashboardErrorBinding, position: Int) {
        viewBinding.errorText.text = viewBinding.root.context.getString(error.errorMessage)
    }

    override fun isSameAs(other: Item<*>): Boolean =
        other is StatusDashboardErrorItem && other.error == error

    override fun hasSameContentAs(other: Item<*>) =
        other is StatusDashboardErrorItem && other.error == error
}
