/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.about

import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemIllustratedBoxBinding
import nl.rijksoverheid.en.items.BaseBindableItem

open class IllustratedBoxItem(
    @StringRes private val title: Int,
    @StringRes private val subtitle: Int,
    @DrawableRes private val illustration: Int,
    @ColorRes private val backgroundTint: Int
) : BaseBindableItem<ItemIllustratedBoxBinding>() {
    data class ViewState(
        @StringRes val title: Int,
        @StringRes val subtitle: Int,
        @DrawableRes val illustration: Int,
        @ColorInt val backgroundTint: Int
    )

    override fun getLayout() = R.layout.item_illustrated_box
    override fun bind(viewBinding: ItemIllustratedBoxBinding, position: Int) {
        viewBinding.viewState = ViewState(
            title, subtitle, illustration, viewBinding.root.context.getColor(backgroundTint)
        )
    }

    override fun isClickable() = true
    override fun isSameAs(other: Item<*>): Boolean =
        other is IllustratedBoxItem && other.title == title

    override fun hasSameContentAs(other: Item<*>) =
        other is IllustratedBoxItem && other.title == title
}
