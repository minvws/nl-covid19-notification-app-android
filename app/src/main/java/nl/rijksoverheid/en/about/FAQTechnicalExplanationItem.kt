/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.about

import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemTechnicalExplanationBinding
import nl.rijksoverheid.en.items.BaseBindableItem

class FAQTechnicalExplanationItem : BaseBindableItem<ItemTechnicalExplanationBinding>() {
    override fun getLayout() = R.layout.item_technical_explanation
    override fun bind(viewBinding: ItemTechnicalExplanationBinding, position: Int) {}
    override fun isClickable() = true
    override fun isSameAs(other: Item<*>): Boolean = other is FAQTechnicalExplanationItem
    override fun hasSameContentAs(other: Item<*>) = other is FAQTechnicalExplanationItem
}
