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
import nl.rijksoverheid.en.databinding.ItemHeaderBinding

class HeaderItem(@StringRes private val text: Int) : BaseBindableItem<ItemHeaderBinding>() {
    override fun getLayout() = R.layout.item_header

    override fun bind(viewBinding: ItemHeaderBinding, position: Int) {
        viewBinding.text = text
    }

    override fun isSameAs(other: Item<*>): Boolean = other is HeaderItem && other.text == text
    override fun hasSameContentAs(other: Item<*>) = other is HeaderItem && other.text == text
}
