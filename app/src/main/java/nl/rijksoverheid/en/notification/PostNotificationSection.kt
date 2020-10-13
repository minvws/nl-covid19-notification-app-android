/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.notification

import com.xwray.groupie.Section
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.model.ResourceBundle
import nl.rijksoverheid.en.items.IllustrationItem
import nl.rijksoverheid.en.items.ResourceBundleHeaderItem
import nl.rijksoverheid.en.items.ResourceBundleParagraphItem

class PostNotificationSection : Section() {
    init {
        setHeader(IllustrationItem(R.drawable.illustration_post_notification))
        setHideWhenEmpty(true)
    }

    fun setGuidance(guidance: List<ResourceBundle.Guidance.Element>) {
        update(guidance.mapNotNull {
            when (it) {
                is ResourceBundle.Guidance.Element.Paragraph -> listOf(
                    ResourceBundleHeaderItem(it.title),
                    ResourceBundleParagraphItem(it.body)
                )
                else -> null
            }
        }.flatten())
    }
}
