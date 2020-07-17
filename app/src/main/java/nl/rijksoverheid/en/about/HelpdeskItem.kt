/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.about

import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemHelpdeskBinding
import nl.rijksoverheid.en.items.BaseBindableItem

class HelpdeskItem : BaseBindableItem<ItemHelpdeskBinding>() {
    override fun getLayout() = R.layout.item_helpdesk
    override fun bind(viewBinding: ItemHelpdeskBinding, position: Int) {}
    override fun isClickable() = true
    override fun isSameAs(other: Item<*>): Boolean = other is HelpdeskItem
    override fun hasSameContentAs(other: Item<*>) = other is HelpdeskItem
}
