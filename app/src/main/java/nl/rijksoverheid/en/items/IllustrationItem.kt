/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.items

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemIllustrationBinding

class IllustrationItem(
    @DrawableRes val image: Int,
    @StringRes val contentDescription: Int? = null
) : BaseBindableItem<ItemIllustrationBinding>() {
    override fun getLayout() = R.layout.item_illustration

    override fun bind(viewBinding: ItemIllustrationBinding, position: Int) {
        viewBinding.image = image
        viewBinding.contentDescription =
            contentDescription?.let { viewBinding.root.context.getString(it) }
    }

    override fun isSameAs(other: Item<*>): Boolean =
        other is IllustrationItem && other.image == image

    override fun hasSameContentAs(other: Item<*>) =
        other is IllustrationItem && other.image == image
}
