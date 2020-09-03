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
import nl.rijksoverheid.en.databinding.ItemInlineIllustrationBinding

class InlineIllustrationItem(
    @DrawableRes private val image: Int,
    @StringRes private val contentDescription: Int,
    @StringRes private val caption: Int?
) : BaseBindableItem<ItemInlineIllustrationBinding>() {

    data class ViewState(
        @DrawableRes val image: Int,
        val contentDescription: String,
        val caption: String?
    )

    override fun getLayout() = R.layout.item_inline_illustration

    override fun bind(viewBinding: ItemInlineIllustrationBinding, position: Int) {
        viewBinding.viewState = ViewState(
            image,
            viewBinding.root.context.getString(contentDescription),
            caption?.let { viewBinding.root.context.getString(it) }
        )
    }

    override fun isSameAs(other: Item<*>): Boolean =
        other is InlineIllustrationItem && other.image == image

    override fun hasSameContentAs(other: Item<*>) =
        other is InlineIllustrationItem && other.image == image
}
