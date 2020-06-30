/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.about

import androidx.annotation.StringRes
import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemFaqHeaderBinding
import nl.rijksoverheid.en.items.BaseBindableItem

class FAQHeaderItem(@StringRes private val text: Int) : BaseBindableItem<ItemFaqHeaderBinding>() {
    override fun getLayout() = R.layout.item_faq_header

    override fun bind(viewBinding: ItemFaqHeaderBinding, position: Int) {
        viewBinding.text = text
    }

    override fun isSameAs(other: Item<*>): Boolean = other is FAQHeaderItem && other.text == text
    override fun hasSameContentAs(other: Item<*>) = other is FAQHeaderItem && other.text == text
}
