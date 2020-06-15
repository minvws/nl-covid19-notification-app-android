/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.items

import android.view.View.OnClickListener
import androidx.annotation.StringRes
import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemButtonBinding

class ButtonItem(
    @StringRes val text: Int,
    val buttonClickListener: () -> Unit
) :
    BaseBindableItem<ItemButtonBinding>() {
    override fun getLayout() = R.layout.item_button

    override fun bind(viewBinding: ItemButtonBinding, position: Int) {
        viewBinding.text = text
        viewBinding.buttonClickListener = OnClickListener { buttonClickListener() }
    }

    override fun isSameAs(other: Item<*>): Boolean = other is ButtonItem && other.text == text
    override fun hasSameContentAs(other: Item<*>) = other is ButtonItem && other.text == text
}
