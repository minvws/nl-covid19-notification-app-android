/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import android.content.Context
import android.text.Spanned
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.accessibility.AccessibilityEvent
import com.google.android.material.textview.MaterialTextView

/**
 * Subclass of MaterialTextView which is capable of displaying HTML.
 * HTML should be parsed to a Spanned object, which can be displayed directly by using the text attribute.
 *
 * For improved accessibility, the `dispatchPopulateAccessibilityEvent` method is overridden.
 * The first clickable span is activated if:
 * 1. TYPE_VIEW_CLICKED event is dispatched
 * 2. User is using touch exploration
 */
class HtmlTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : MaterialTextView(context, attrs, defStyle, defStyleRes) {

    /**
     * This method is overriden to add support for activating links when using touch exploration
     */
    override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent?): Boolean {
        // Check if eventType is TYPE_VIEW_CLICKED
        if (event == null || event.eventType != AccessibilityEvent.TYPE_VIEW_CLICKED) {
            return super.dispatchPopulateAccessibilityEvent(event)
        }

        // Check if touch exploration is enabled (e.g. TalkBack)
        if (!Accessibility.touchExploration(context)) {
            return super.dispatchPopulateAccessibilityEvent(event)
        }

        // Try to get text as Spanned object
        (text as? Spanned)?.let { spanned ->
            // Extract all ClickableSpan instances
            val clickableSpans = spanned.getSpans(0, spanned.length, ClickableSpan::class.java)

            // Activate the first clickable span, if it exists
            clickableSpans.firstOrNull()?.onClick(this)
        }

        return super.dispatchPopulateAccessibilityEvent(event)
    }
}
