/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.databinding

import android.content.res.Configuration
import android.content.res.Resources
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.databinding.BindingAdapter
import com.airbnb.lottie.LottieAnimationView
import nl.rijksoverheid.en.util.HtmlTextViewWidget
import nl.rijksoverheid.en.util.ext.formatPauseDuration
import java.time.LocalDateTime

/**
 * Custom set of data binding properties used by CoronaMelder app.
 */
object BindingAdapters {

    /**
     * Combines a set of custom properties for setting the visibility in certain situations.
     *
     * @param view: used view to bind the visibility properties
     * @param show: show or hide the view
     * @param showInNightMode: show or hide in night mode
     * @param showInDayMode: show or hide in day mode
     * @param keepInLayout: sets the visibility to INVISIBLE or GONE if the view should not be shown
     * @param hideOnSmallScreenHeight: hides the view if the screen has a small height
     * or when the view is a [ImageView] containing an image that fills up the whole screen.
     */
    @JvmStatic
    @BindingAdapter(
        "show",
        "showInNightMode",
        "showInDayMode",
        "keepInLayout",
        "hideOnSmallScreenHeight",
        requireAll = false
    )
    fun show(
        view: View,
        show: Boolean?,
        showInNightMode: Boolean?,
        showInDayMode: Boolean?,
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

        val isUsingNightMode = view.context.resources.isUsingNightModeResources()

        val visibility = when {
            hideSmallScreen -> {
                View.GONE
            }
            show == true ||
                (showInNightMode == true && isUsingNightMode) ||
                (showInDayMode == true && !isUsingNightMode) -> {
                View.VISIBLE
            }
            show == false ||
                (showInNightMode == false && isUsingNightMode) ||
                (showInDayMode == false && !isUsingNightMode) -> {
                if (keepInLayout) View.INVISIBLE else View.GONE
            }
            else -> {
                null
            }
        }

        visibility?.let { view.visibility = it }
    }

    private fun Resources.isUsingNightModeResources(): Boolean {
        return when (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_UNDEFINED -> false
            else -> false
        }
    }

    private fun isSmallScreen(configuration: Configuration): Boolean {
        return configuration.screenHeightDp <= 480 || configuration.fontScale >= 1.3
    }

    /**
     * Check if image would fill more than 90% of the screen when scaled full width while keeping the aspect ratio.
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

    /**
     * Updates the text in a HtmlTextViewWidget.
     *
     * @param view: the HtmlTextViewWidget
     * @param htmlText: string containing html tags
     * @param enableHtmlLinks: configure if url spans should be clickable or not
     */
    @JvmStatic
    @BindingAdapter("htmlText", "enableHtmlLinks", requireAll = false)
    fun setHtmlText(view: HtmlTextViewWidget, htmlText: String?, enableHtmlLinks: Boolean?) {
        view.setHtmlText(htmlText, enableHtmlLinks ?: false)
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

    /**
     * Mark view as button for accessibility purposes.
     */
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

    /**
     * Bind LocalDateTime to HtmlTextViewWidget and format it.
     */
    @JvmStatic
    @BindingAdapter("pausedState")
    fun bindPausedState(view: HtmlTextViewWidget, pausedUntil: LocalDateTime?) {
        val formattedDuration = pausedUntil?.formatPauseDuration(view.context)
        if (view.text != formattedDuration)
            view.setHtmlText(formattedDuration)
    }

    @JvmStatic
    @BindingAdapter("app:tint")
    fun ImageView.setImageTint(@ColorInt color: Int) {
        setColorFilter(color)
    }
}
