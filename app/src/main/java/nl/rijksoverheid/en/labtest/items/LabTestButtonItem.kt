/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest.items

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemLabTestButtonBinding
import nl.rijksoverheid.en.items.BaseBindableItem

class LabTestButtonItem(
    @StringRes private val text: Int,
    @DrawableRes private val drawableEnd: Int?,
    private val buttonClickListener: () -> Unit,
    private val enabled: Boolean = true
) : BaseBindableItem<ItemLabTestButtonBinding>() {
    data class ViewState(
        @StringRes val text: Int,
        val drawableEnd: Drawable?,
        val enabled: Boolean,
        val click: () -> Unit
    )

    override fun getLayout() = R.layout.item_lab_test_button

    override fun bind(viewBinding: ItemLabTestButtonBinding, position: Int) {
        val context = viewBinding.root.context
        viewBinding.viewState = ViewState(
            text = text,
            drawableEnd = drawableEnd?.let { ContextCompat.getDrawable(context, it) },
            enabled = enabled,
            click = buttonClickListener
        )
    }

    override fun isSameAs(other: Item<*>): Boolean = other is LabTestButtonItem && other.text == text
    override fun hasSameContentAs(other: Item<*>) = other is LabTestButtonItem && other.text == text &&
        other.enabled == enabled
}
