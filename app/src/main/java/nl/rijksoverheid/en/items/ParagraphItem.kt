/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.items

import androidx.annotation.StringRes
import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemParagraphBinding

class ParagraphItem(
    @StringRes private val text: Int,
    private vararg val formatArgs: String,
    private val clickable: Boolean = false
) : BaseBindableItem<ItemParagraphBinding>() {
    override fun getLayout() = R.layout.item_paragraph

    override fun bind(viewBinding: ItemParagraphBinding, position: Int) {
        viewBinding.content.setHtmlText(viewBinding.root.context.getString(text, *formatArgs))
        if (clickable) {
            viewBinding.content.enableCustomLinks {
                viewBinding.root.callOnClick()
            }
        }
    }

    override fun isClickable() = clickable
    override fun isSameAs(other: Item<*>): Boolean = other is ParagraphItem && other.text == text
    override fun hasSameContentAs(other: Item<*>) = other is ParagraphItem && other.text == text
}
