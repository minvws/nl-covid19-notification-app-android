/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.about

import com.xwray.groupie.Section
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.about.FAQItemId.ANONYMOUS
import nl.rijksoverheid.en.about.FAQItemId.BLUETOOTH
import nl.rijksoverheid.en.about.FAQItemId.LOCATION
import nl.rijksoverheid.en.about.FAQItemId.NOTIFICATION
import nl.rijksoverheid.en.about.FAQItemId.POWER_USAGE
import nl.rijksoverheid.en.items.HeaderItem
import nl.rijksoverheid.en.items.ParagraphItem

enum class FAQItemId { LOCATION, ANONYMOUS, NOTIFICATION, BLUETOOTH, POWER_USAGE }

class FAQDetailSections {
    fun getSection(faqItemId: FAQItemId) = when (faqItemId) {
        LOCATION -> Section(
            listOf(
                HeaderItem(R.string.faq_location),
                ParagraphItem(R.string.faq_location_paragraph_1)
            )
        )
        ANONYMOUS -> Section(
            listOf(
                HeaderItem(R.string.faq_anonymous),
                ParagraphItem(R.string.faq_anonymous_paragraph_1),
                ParagraphItem(R.string.faq_anonymous_paragraph_2)
            )
        )
        NOTIFICATION -> Section(
            listOf(
                HeaderItem(R.string.faq_notification),
                ParagraphItem(R.string.faq_notification_paragraph_1)
            )
        )
        BLUETOOTH -> Section(
            listOf(
                HeaderItem(R.string.faq_bluetooth),
                ParagraphItem(R.string.faq_bluetooth_paragraph_1)
            )
        )
        POWER_USAGE -> Section(
            listOf(
                HeaderItem(R.string.faq_power_usage),
                ParagraphItem(R.string.faq_power_usage_paragraph_1)
            )
        )
    }
}
