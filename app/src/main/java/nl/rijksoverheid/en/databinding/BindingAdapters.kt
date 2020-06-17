/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.databinding

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.text.HtmlCompat
import androidx.databinding.BindingAdapter

object BindingAdapters {
    @JvmStatic
    @BindingAdapter("show")
    fun show(view: View, show: Boolean) {
        view.visibility = if (show) View.VISIBLE else View.GONE
    }

    @JvmStatic
    @BindingAdapter("drawableTopCompat")
    fun setDrawableTopCompat(view: TextView, drawable: Int?) {
        drawable?.let {
            view.setCompoundDrawablesWithIntrinsicBounds(
                null,
                AppCompatResources.getDrawable(view.context, drawable),
                null,
                null
            )
        }
    }

    @JvmStatic
    @BindingAdapter("showIfLabelSet")
    fun showIfLabelSet(button: Button, @StringRes label: Int) {
        if (label == 0) {
            button.visibility = View.GONE
        } else {
            button.visibility = View.VISIBLE
            button.setText(label)
        }
    }

    @JvmStatic
    @BindingAdapter("htmlText")
    fun setHtmlText(view: TextView, htmlText: String) {
        view.text = HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
}
