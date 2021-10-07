/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import android.content.Context
import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.text.getSpans
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.widget.TextViewCompat
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.util.ext.enableCustomLinks
import nl.rijksoverheid.en.util.ext.enableHtmlLinks
import nl.rijksoverheid.en.util.ext.hasUrlSpans
import nl.rijksoverheid.en.util.ext.isHeading
import nl.rijksoverheid.en.util.ext.isListItem
import nl.rijksoverheid.en.util.ext.separated
import nl.rijksoverheid.en.util.spans.BulletPointSpan

/**
 * The HtmlTextViewWidget is able to display (simple) HTML in an accessible way.
 * 1. HTML is parsed to a Spanned object.
 * 2. the Spanned object is split on the linebreak character.
 * 3. Each Spanned object is then displayed using a `HtmlTextView`.
 * 4. Accessibility attributes are applied to the HtmlTextView.
 *
 * The methods enableHtmlLinks() and enableCustomLinks() are dispatched to each TextView subview.
 * The getSpannableFromHtml() method parses HTML into a Spannable object while taking legacy implementations into account.
 */
class HtmlTextViewWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0,
) : LinearLayout(context, attrs, defStyle, defStyleRes) {

    // Reflects the full text shown in the subviews. Can only be set internally.
    var text: CharSequence? = null
        private set

    private val mGravity: Int

    @StyleRes
    private val textAppearance: Int

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.HtmlTextViewWidget,
            defStyle,
            defStyleRes
        ).apply {
            try {
                mGravity = getInt(R.styleable.HtmlTextViewWidget_android_gravity, Gravity.START or Gravity.TOP)
                textAppearance = getResourceId(R.styleable.HtmlTextViewWidget_android_textAppearance, R.style.TextAppearance_App_Body1)
            } finally {
                recycle()
            }
        }
        orientation = VERTICAL
    }

    /**
     * Sets the text based on a string resource.
     * Links are disabled by default, but can be enabled.
     */
    fun setHtmlText(htmlText: Int, htmlLinksEnabled: Boolean = false) {
        val text = context.getString(htmlText)
        setHtmlText(text, htmlLinksEnabled = htmlLinksEnabled)
    }

    /**
     * Sets the text based on a string.
     * Links are disabled by default, but can be enabled.
     */
    fun setHtmlText(htmlText: String?, htmlLinksEnabled: Boolean = false) {
        removeAllViews()

        if (htmlText.isNullOrBlank()) {
            return
        }

        // Step 1: Parse the given String into a Spannable
        val spannable = getSpannableFromHtml(htmlText)
        text = spannable

        // Step 2: Separate the Spannable on each linebreak
        val parts = spannable.separated("\n")

        // Step 3: Add a HtmlTextView for each part of the Spannable
        val iterator = parts.iterator()
        while (iterator.hasNext()) {
            val part = iterator.next()

            val textView = HtmlTextView(context)
            textView.text = part

            // Only mark as heading if text also contains content
            if (part.isHeading && iterator.hasNext()) {
                ViewCompat.setAccessibilityHeading(textView, true)
            }

            val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            if (iterator.hasNext()) {
                val marginBottom = if (part.isHeading || part.isListItem) {
                    textView.lineHeight / 4 // Headings and list items have a quarter of the default margin
                } else {
                    textView.lineHeight // By default, the line height is used as bottom bottom
                }
                params.setMargins(0, 0, 0, marginBottom)
            }

            textView.layoutParams = params
            textView.gravity = mGravity

            TextViewCompat.setTextAppearance(textView, textAppearance)

            addView(textView)
        }

        // Step 4: Enable links, if enabled
        if (htmlLinksEnabled) {
            enableHtmlLinks()
        }
    }

    /**
     * Enables HTML links for all of it's TextView subviews
     */
    fun enableHtmlLinks() {
        children.filterIsInstance(TextView::class.java).forEach { textView ->
            if (textView.hasUrlSpans())
                textView.enableHtmlLinks()
        }
    }

    /**
     * Enables custom links for all of it's TextView subviews
     */
    fun enableCustomLinks(onLinkClick: () -> Unit) {
        children.filterIsInstance(TextView::class.java).forEach { textView ->
            if (textView.hasUrlSpans())
                textView.enableCustomLinks(onLinkClick)
        }
    }

    /**
     * Parses a String into a Spannable object.
     * It takes both legacy and modern HTML parsing implementations into account.
     */
    private fun getSpannableFromHtml(html: String): Spannable {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getSpannableFromHtmlModern(html)
        } else {
            getSpannableFromHtmlLegacy(html)
        }
    }

    /**
     * Parses a String into a Spannable object.
     * It then replaces all BulletSpan's with BulletPointSpans for consistent styling.
     */
    private fun getSpannableFromHtmlModern(html: String): Spannable {
        val spannableBuilder = getSpannableFromHtmlLegacy(html)

        spannableBuilder.getSpans<BulletSpan>().forEach {
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
     * Parses a String into a Spannable object.
     * During parsing, BulletSpans are replaced with a temporary Bullet marker.
     * This is done because using BulletSpan directly can cause crashes on Android 6.
     * After parsing, all Bullets are replaced with BulletSpans.
     */
    private fun getSpannableFromHtmlLegacy(html: String): Spannable {
        // marker object. We can't directly use BulletSpan as this crashes on Android 6
        class Bullet

        val htmlSpannable = HtmlCompat.fromHtml(
            html,
            HtmlCompat.FROM_HTML_MODE_COMPACT,
            null,
            { opening, tag, output, _ ->
                if (tag == "li" && opening) {
                    output.setSpan(
                        Bullet(),
                        output.length,
                        output.length,
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                    )
                }
                if (tag == "ul" && opening) {
                    // add a line break if this tag is not on a new line
                    if (output.isNotEmpty()) {
                        output.append("\n")
                    }
                }
                if (tag == "li" && !opening) {
                    output.append("\n")
                    val lastMark =
                        output.getSpans<Bullet>().lastOrNull()
                    lastMark?.let {
                        val start = output.getSpanStart(it)
                        output.removeSpan(it)
                        if (start != output.length) {
                            output.setSpan(
                                it,
                                start,
                                output.length,
                                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                            )
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
            spannableBuilder.setSpan(
                BulletSpan(
                    context.resources.getDimensionPixelSize(R.dimen.bullet_gap_size)
                ),
                start,
                end,
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }

        return spannableBuilder
    }
}
