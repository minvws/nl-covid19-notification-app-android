/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util.spans

import android.text.style.URLSpan
import android.view.View

class CallbackUrlSpan(val onLinkClick: () -> Unit) : URLSpan("") {
    override fun onClick(widget: View) {
        super.onClick(widget)
        onLinkClick.invoke()
    }
}
