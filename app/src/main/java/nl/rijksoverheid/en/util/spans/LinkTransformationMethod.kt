/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util.spans

import android.graphics.Rect
import android.text.Spannable
import android.text.Spanned
import android.text.method.TransformationMethod
import android.text.style.TextAppearanceSpan
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView
import nl.rijksoverheid.en.R

class LinkTransformationMethod(private val method: Method) : TransformationMethod {

    sealed class Method {
        object WebLinks : Method()
        data class CustomLinks(val onLinkClick: () -> Unit) : Method()
    }

    override fun getTransformation(source: CharSequence?, view: View?): CharSequence {
        if (view is TextView) {
            if (view.text == null || view.text !is Spannable) {
                return source ?: ""
            }
            val text = view.text as Spannable
            val spans = text.getSpans(0, view.length(), URLSpan::class.java)

            spans.forEach { oldSpan ->
                val start = text.getSpanStart(oldSpan)
                val end = text.getSpanEnd(oldSpan)
                val url = oldSpan.url
                text.removeSpan(oldSpan)

                when (method) {
                    is Method.CustomLinks -> {
                        text.setSpan(
                            CallbackUrlSpan(method.onLinkClick),
                            start,
                            end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    is Method.WebLinks -> {
                        text.setSpan(
                            ChromeCustomTabsUrlSpan(url),
                            start,
                            end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }

                text.setSpan(
                    TextAppearanceSpan(
                        view.context,
                        R.style.TextAppearance_App_Link
                    ),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            return text
        }
        return source ?: ""
    }

    override fun onFocusChanged(
        view: View?,
        sourceText: CharSequence?,
        focused: Boolean,
        direction: Int,
        previouslyFocusedRect: Rect?
    ) {
    }
}
