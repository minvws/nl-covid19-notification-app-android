/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.items

import android.view.View
import android.widget.Button
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemStatusActionBinding

open class ActionItem(
    @DrawableRes private val icon: Int,
    @StringRes private val title: Int,
    @StringRes private val subtitle: Int
) : BaseBindableItem<ItemStatusActionBinding>() {
    override fun getLayout() = R.layout.item_status_action
    override fun isClickable() = true

    override fun bind(viewBinding: ItemStatusActionBinding, position: Int) {
        ViewCompat.setAccessibilityDelegate(
            viewBinding.container,
            object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfoCompat
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.contentDescription =
                        "${viewBinding.statusTitle.text}\n${viewBinding.statusSubtitle.text}"
                    info.className = Button::class.java.name
                }
            }
        )
        viewBinding.infoIcon = icon
        viewBinding.infoTitle = title
        viewBinding.infoSubtitle = subtitle
    }
}
