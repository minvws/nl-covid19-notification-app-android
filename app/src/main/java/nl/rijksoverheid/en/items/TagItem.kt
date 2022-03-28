/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.items

import androidx.annotation.StringRes
import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemTagBinding

class TagItem(@StringRes private val text: Int, private vararg val formatArgs: String) :
    BaseBindableItem<ItemTagBinding>() {
    override fun getLayout() = R.layout.item_tag

    override fun bind(viewBinding: ItemTagBinding, position: Int) {
        viewBinding.text = viewBinding.root.context.getString(text, *formatArgs)
    }

    override fun isSameAs(other: Item<*>): Boolean =
        other is TagItem && other.text == text && other.formatArgs.contentEquals(formatArgs)

    override fun hasSameContentAs(other: Item<*>) =
        other is TagItem && other.text == text && other.formatArgs.contentEquals(formatArgs)
}
