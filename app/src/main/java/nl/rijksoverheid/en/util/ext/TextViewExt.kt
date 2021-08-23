/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util.ext

import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.widget.TextView
import androidx.core.text.getSpans
import nl.rijksoverheid.en.util.spans.LinkTransformationMethod

fun TextView.enableHtmlLinks() {
    this.transformationMethod =
        LinkTransformationMethod(method = LinkTransformationMethod.Method.WebLinks)
    this.movementMethod = LinkMovementMethod.getInstance()
}

fun TextView.enableCustomLinks(onLinkClick: () -> Unit) {
    this.transformationMethod =
        LinkTransformationMethod(method = LinkTransformationMethod.Method.CustomLinks(onLinkClick))
    this.movementMethod = LinkMovementMethod.getInstance()
}

fun TextView.hasUrlSpans(): Boolean {
    return (text as? Spanned)?.getSpans<URLSpan>()?.any() == true
}
