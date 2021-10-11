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
import nl.rijksoverheid.en.databinding.ItemTalkbackOnlyHeaderBinding

class TalkbackOnlyHeaderItem(@StringRes private val text: Int) : BaseBindableItem<ItemTalkbackOnlyHeaderBinding>() {
    override fun getLayout() = R.layout.item_talkback_only_header

    override fun bind(viewBinding: ItemTalkbackOnlyHeaderBinding, position: Int) {
        viewBinding.text = viewBinding.root.context.getString(text)
    }

    override fun isSameAs(other: Item<*>): Boolean =
        other is TalkbackOnlyHeaderItem && other.text == text

    override fun hasSameContentAs(other: Item<*>) =
        other is TalkbackOnlyHeaderItem && other.text == text
}
