/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.items

import android.text.Html
import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemParagraphBinding

class BulletedListItem(
    @StringRes val text: Int
) : BaseBindableItem<ItemParagraphBinding>() {
    override fun getLayout() = R.layout.item_paragraph

    override fun bind(viewBinding: ItemParagraphBinding, position: Int) {
        val html = viewBinding.root.context.getString(text)

        val htmlSpannable = HtmlCompat.fromHtml(
            html,
            HtmlCompat.FROM_HTML_MODE_COMPACT,
            null,
            Html.TagHandler { opening, tag, output, _ ->
                class Bullet

                if (tag == "li" && opening) {
                    output.setSpan(Bullet(), output.length, output.length, SPAN_INCLUSIVE_EXCLUSIVE)
                }
                if (tag == "li" && !opening) {
                    output.append("\n")
                    val lastMark =
                        output.getSpans(0, output.length, Bullet::class.java).lastOrNull()
                    lastMark?.let {
                        val start = output.getSpanStart(it)
                        output.removeSpan(it)
                        if (start != output.length) {
                            output.setSpan(
                                BulletSpan(),
                                start,
                                output.length,
                                SPAN_INCLUSIVE_EXCLUSIVE
                            )
                        }
                    }
                }
            })
        val spannableBuilder = SpannableStringBuilder(htmlSpannable)
        val bulletSpans =
            spannableBuilder.getSpans(0, spannableBuilder.length, BulletSpan::class.java)
        bulletSpans.forEach {
            val start = spannableBuilder.getSpanStart(it)
            val end = spannableBuilder.getSpanEnd(it)
            spannableBuilder.removeSpan(it)
            spannableBuilder.setSpan(
                BulletPointSpan(
                    gapWidth = viewBinding.root.context.resources.getDimensionPixelSize(R.dimen.bullet_gap_size),
                    bulletRadius = viewBinding.root.context.resources.getDimension(R.dimen.bullet_radius),
                    color = ContextCompat.getColor(viewBinding.root.context, R.color.color_primary)
                ),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        viewBinding.text = spannableBuilder
    }

    override fun isSameAs(other: Item<*>): Boolean = other is BulletedListItem && other.text == text
    override fun hasSameContentAs(other: Item<*>) = other is BulletedListItem && other.text == text
}
