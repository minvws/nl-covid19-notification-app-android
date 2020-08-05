/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.items

import android.text.SpannableString
import android.text.Spanned
import android.text.style.TextAppearanceSpan
import android.text.style.URLSpan
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import androidx.core.text.getSpans
import androidx.core.view.ViewCompat
import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemParagraphBinding

class ParagraphItem(
    @StringRes private val text: Int,
    private vararg val formatArgs: Any,
    private val clickable: Boolean = false
) : BaseBindableItem<ItemParagraphBinding>() {
    override fun getLayout() = R.layout.item_paragraph

    override fun bind(viewBinding: ItemParagraphBinding, position: Int) {
        ViewCompat.enableAccessibleClickableSpanSupport(viewBinding.content)
        viewBinding.text = SpannableString(
            HtmlCompat.fromHtml(
                viewBinding.root.context.getString(text, *formatArgs),
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
        ).apply {
            getSpans<URLSpan>().forEach {
                val start = getSpanStart(it)
                val end = getSpanEnd(it)
                setSpan(
                    TextAppearanceSpan(viewBinding.root.context, R.style.TextAppearance_App_Link),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    override fun isClickable() = clickable
    override fun isSameAs(other: Item<*>): Boolean = other is ParagraphItem && other.text == text
    override fun hasSameContentAs(other: Item<*>) = other is ParagraphItem && other.text == text
}
