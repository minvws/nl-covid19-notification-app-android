/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.items

import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemParagraphBinding

class ResourceBundleParagraphItem(
    private val text: String
) : BaseBindableItem<ItemParagraphBinding>() {
    override fun getLayout() = R.layout.item_paragraph

    override fun bind(viewBinding: ItemParagraphBinding, position: Int) {
        viewBinding.content.setHtmlText(text, true)
    }

    override fun isSameAs(other: Item<*>): Boolean =
        other is ResourceBundleParagraphItem && other.text == text

    override fun hasSameContentAs(other: Item<*>) =
        other is ResourceBundleParagraphItem && other.text == text
}
