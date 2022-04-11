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
import nl.rijksoverheid.en.databinding.ItemLinkBinding

open class LinkItem(
    @StringRes private val text: Int,
    private vararg val formatArgs: String,
    private val onClick: () -> Unit
) :
    BaseBindableItem<ItemLinkBinding>() {

    override fun getLayout() = R.layout.item_link

    override fun bind(viewBinding: ItemLinkBinding, position: Int) {
        viewBinding.content.text = viewBinding.root.context.getString(text, *formatArgs)
        viewBinding.content.setOnClickListener { onClick() }
    }

    override fun isSameAs(other: Item<*>): Boolean =
        other is LinkItem && other.text == text && other.formatArgs.contentEquals(formatArgs)

    override fun hasSameContentAs(other: Item<*>) =
        other is LinkItem && other.text == text && other.formatArgs.contentEquals(formatArgs)
}
