/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.items

import android.net.Uri
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.TextAppearanceSpan
import android.text.style.URLSpan
import android.view.View
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.text.getSpans
import androidx.core.view.ViewCompat
import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemParagraphBinding
import nl.rijksoverheid.en.util.fromHtmlWithCustomReplacements

class ResourceBundleParagraphItem(
    private val text: String
) : BaseBindableItem<ItemParagraphBinding>() {
    override fun getLayout() = R.layout.item_paragraph

    override fun bind(viewBinding: ItemParagraphBinding, position: Int) {
        ViewCompat.enableAccessibleClickableSpanSupport(viewBinding.content)
        viewBinding.content.movementMethod = LinkMovementMethod.getInstance()
        viewBinding.text = fromHtmlWithCustomReplacements(
            viewBinding.root.context,
            text.replace("\n", "<br/>")
        ).apply {
            getSpans<URLSpan>().forEach {
                val start = getSpanStart(it)
                val end = getSpanEnd(it)
                setSpan(
                    object : URLSpan(it.url) {
                        override fun onClick(widget: View) {
                            CustomTabsIntent.Builder().build().launchUrl(widget.context, Uri.parse(url))
                        }
                    },
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                setSpan(
                    TextAppearanceSpan(
                        viewBinding.root.context,
                        R.style.TextAppearance_App_Link
                    ),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                removeSpan(it)
            }
        }
    }

    override fun isSameAs(other: Item<*>): Boolean =
        other is ResourceBundleParagraphItem && other.text == text

    override fun hasSameContentAs(other: Item<*>) =
        other is ResourceBundleParagraphItem && other.text == text
}
