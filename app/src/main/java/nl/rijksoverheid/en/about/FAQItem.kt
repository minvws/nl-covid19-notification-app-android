/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.about

import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemFaqBinding
import nl.rijksoverheid.en.items.BaseBindableItem

class FAQItem(
    val id: FAQItemId
) :
    BaseBindableItem<ItemFaqBinding>() {
    override fun getLayout() = R.layout.item_faq

    override fun bind(viewBinding: ItemFaqBinding, position: Int) {
        viewBinding.text = id.label
    }

    override fun isClickable() = true
    override fun isSameAs(other: Item<*>): Boolean = other is FAQItem && other.id == id
    override fun hasSameContentAs(other: Item<*>) = other is FAQItem && other.id == id
}
