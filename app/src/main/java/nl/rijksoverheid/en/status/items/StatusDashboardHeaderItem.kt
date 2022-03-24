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
import nl.rijksoverheid.en.databinding.ItemStatusDashboardHeaderBinding
import nl.rijksoverheid.en.items.BaseBindableItem

object StatusDashboardHeaderItem : BaseBindableItem<ItemStatusDashboardHeaderBinding>() {

    override fun getLayout() = R.layout.item_status_dashboard_header

    override fun bind(viewBinding: ItemStatusDashboardHeaderBinding, position: Int) {}
    override fun isSameAs(other: Item<*>): Boolean = other is StatusDashboardHeaderItem
    override fun hasSameContentAs(other: Item<*>) = other is StatusDashboardHeaderItem
}
