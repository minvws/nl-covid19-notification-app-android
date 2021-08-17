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

class BulletedListItem(
    @StringRes private val text: Int,
    private vararg val formatArgs: String
) : BaseBindableItem<ItemParagraphBinding>() {
    override fun getLayout() = R.layout.item_paragraph

    override fun bind(viewBinding: ItemParagraphBinding, position: Int) {
        val html = viewBinding.root.context.getString(text, *formatArgs)
        viewBinding.content.setHtmlText(html)
    }

    override fun isSameAs(other: Item<*>): Boolean =
        other is BulletedListItem && other.text == text && other.formatArgs.contentEquals(
            formatArgs
        )

    override fun hasSameContentAs(other: Item<*>) =
        other is BulletedListItem && other.text == text && other.formatArgs.contentEquals(formatArgs)
}
