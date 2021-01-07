/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.databinding

import android.content.res.Configuration
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.databinding.BindingAdapter
import com.airbnb.lottie.LottieAnimationView
import nl.rijksoverheid.en.util.fromHtmlWithCustomReplacements

object BindingAdapters {
    @JvmStatic
    @BindingAdapter("show", "keepInLayout", "hideOnSmallScreenHeight", requireAll = false)
    fun show(
        view: View,
        show: Boolean?,
        keepInLayout: Boolean,
        hideOnSmallScreenHeight: Boolean?
    ) {
        val hideSmallScreen = (
            hideOnSmallScreenHeight
                ?: false
            ) && (
            isSmallScreen(view.context.resources.configuration) ||
                (view as? ImageView)?.isImageFillingScreen() == true
            )

        val visibility = when {
            hideSmallScreen -> {
                View.GONE
            }
            show == true -> {
                View.VISIBLE
            }
            show == false -> {
                if (keepInLayout) View.INVISIBLE else View.GONE
            }
            else -> {
                null
            }
        }

        visibility?.let { view.visibility = it }
    }

    private fun isSmallScreen(configuration: Configuration): Boolean {
        return configuration.screenHeightDp <= 480 || configuration.fontScale >= 1.3
    }

    /**
     * Check if image would fill more than 80% of the screen when scaled full width while keeping the aspect ratio
     * @return true if the image fills more than 90% of the screen
     */
    private fun ImageView.isImageFillingScreen(): Boolean {
        val ratio = drawable.intrinsicHeight.toFloat() / drawable.intrinsicWidth.toFloat()
        val configuration = context.resources.configuration
        return configuration.screenWidthDp * ratio > configuration.screenHeightDp * 0.9
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
        view.text = fromHtmlWithCustomReplacements(view.context, htmlText)
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

    @JvmStatic
    @BindingAdapter("contentDescriptionRes")
    fun setContentDescriptionRes(view: View, @StringRes stringRes: Int) {
        view.contentDescription = view.context.getString(stringRes)
    }

    @JvmStatic
    @BindingAdapter("markAsButtonForAccessibility")
    fun markAsButtonForAccessibility(view: View, mark: Boolean) {
        if (mark) {
            ViewCompat.setAccessibilityDelegate(
                view,
                object : AccessibilityDelegateCompat() {
                    override fun onInitializeAccessibilityNodeInfo(
                        host: View,
                        info: AccessibilityNodeInfoCompat
                    ) {
                        super.onInitializeAccessibilityNodeInfo(host, info)
                        info.className = Button::class.java.name
                    }
                }
            )
        }
    }
}
