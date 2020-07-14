/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.labtest

import android.view.View
import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemTextButtonBinding
import nl.rijksoverheid.en.items.BaseBindableItem

class TextButtonItem(private val callback: () -> Unit) :
    BaseBindableItem<ItemTextButtonBinding>() {
    override fun getLayout() = R.layout.item_text_button

    override fun bind(viewBinding: ItemTextButtonBinding, position: Int) {
        viewBinding.buttonClickListener = View.OnClickListener { callback.invoke() }
    }

    override fun isSameAs(other: Item<*>): Boolean = other is TextButtonItem
    override fun hasSameContentAs(other: Item<*>) = other is TextButtonItem
}