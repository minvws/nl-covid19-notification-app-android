/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemStatusDashboardLoadingBinding
import nl.rijksoverheid.en.items.BaseBindableItem

object StatusDashboardLoadingItem : BaseBindableItem<ItemStatusDashboardLoadingBinding>() {
    override fun getLayout() = R.layout.item_status_dashboard_loading
    override fun bind(viewBinding: ItemStatusDashboardLoadingBinding, position: Int) {
        // Nothing to bind
    }

    override fun isSameAs(other: Item<*>): Boolean = other is StatusDashboardLoadingItem
    override fun hasSameContentAs(other: Item<*>) = other is StatusDashboardLoadingItem
}
