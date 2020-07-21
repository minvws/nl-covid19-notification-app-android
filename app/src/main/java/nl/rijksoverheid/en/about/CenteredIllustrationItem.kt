/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.about

import androidx.annotation.DrawableRes
import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemCenteredIllustrationBinding
import nl.rijksoverheid.en.items.BaseBindableItem

class CenteredIllustrationItem(
    @DrawableRes private val image: Int
) : BaseBindableItem<ItemCenteredIllustrationBinding>() {
    override fun getLayout() = R.layout.item_centered_illustration

    override fun bind(viewBinding: ItemCenteredIllustrationBinding, position: Int) {
        viewBinding.image = image
    }

    override fun isSameAs(other: Item<*>): Boolean =
        other is CenteredIllustrationItem && other.image == image

    override fun hasSameContentAs(other: Item<*>) =
        other is CenteredIllustrationItem && other.image == image
}
