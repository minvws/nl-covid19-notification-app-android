/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util.spans

import android.net.Uri
import android.text.style.URLSpan
import android.view.View
import androidx.browser.customtabs.CustomTabsIntent

class ChromeCustomTabsUrlSpan(url: String?) : URLSpan(url) {
    override fun onClick(widget: View) {
        super.onClick(widget)
        url?.let {
            val uri = Uri.parse(url)
            CustomTabsIntent.Builder().build().launchUrl(widget.context, uri)
        }
    }
}
