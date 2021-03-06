/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import android.content.Context
import android.text.Html
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.text.getSpans
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.items.BulletPointSpan

fun fromHtmlWithCustomReplacements(context: Context, html: String): Spannable {
    val spannableBuilder = fromHtml(html)
    val bulletSpans =
        spannableBuilder.getSpans<BulletSpan>()

    bulletSpans.forEach {
        val start = spannableBuilder.getSpanStart(it)
        val end = spannableBuilder.getSpanEnd(it)
        spannableBuilder.removeSpan(it)
        spannableBuilder.setSpan(
            BulletPointSpan(
                gapWidth = context.resources.getDimensionPixelSize(R.dimen.bullet_gap_size),
                bulletRadius = context.resources.getDimension(R.dimen.bullet_radius),
                color = ContextCompat.getColor(context, R.color.color_primary)
            ),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    return spannableBuilder
}

/**
 * Parses the html and produces a [SpannableStringBuilder] in a backward compatible way
 */
private fun fromHtml(html: String): SpannableStringBuilder {
    // marker object. We can't directly use BulletSpan as this crashes on Android 6
    class Bullet

    val htmlSpannable = HtmlCompat.fromHtml(
        html,
        HtmlCompat.FROM_HTML_MODE_COMPACT,
        null,
        Html.TagHandler { opening, tag, output, _ ->
            if (tag == "li" && opening) {
                output.setSpan(Bullet(), output.length, output.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }
            if (tag == "ul" && opening && output.isNotEmpty()) {
                // add a line break if this tag is not on a new line
                output.append("\n")
            }
            if (tag == "li" && !opening) {
                output.append("\n")
                val lastMark = output.getSpans<Bullet>().lastOrNull()
                lastMark?.let {
                    val start = output.getSpanStart(it)
                    output.removeSpan(it)
                    if (start != output.length) {
                        output.setSpan(it, start, output.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                    }
                }
            }
        }
    )

    val spannableBuilder = SpannableStringBuilder(htmlSpannable)
    // replace the marker with BulletSpan if the markers have been added
    spannableBuilder.getSpans<Bullet>().forEach {
        val start = spannableBuilder.getSpanStart(it)
        val end = spannableBuilder.getSpanEnd(it)
        spannableBuilder.removeSpan(it)
        spannableBuilder.setSpan(BulletSpan(), start, end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
    }
    return spannableBuilder
}
