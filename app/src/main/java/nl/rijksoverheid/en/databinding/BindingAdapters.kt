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
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import androidx.databinding.BindingAdapter
import com.airbnb.lottie.LottieAnimationView

object BindingAdapters {
    @JvmStatic
    @BindingAdapter("show")
    fun show(view: View, show: Boolean) {
        view.visibility = if (show) View.VISIBLE else View.GONE
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

    @JvmStatic
    @BindingAdapter("optional_animation")
    fun setOptionalAnimation(lottieView: LottieAnimationView, @RawRes src: Int) {
        if (src != 0) {
            lottieView.setAnimation(src)
            // We need to explicitly start the animation when using data binding
            lottieView.playAnimation()
        }
    }
}
