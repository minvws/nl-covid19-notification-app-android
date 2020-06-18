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
import nl.rijksoverheid.en.about.FAQItemId.TECHNICAL

class AboutSection : Section(
    listOf(
        FAQHeaderItem(R.string.faq_header),
        FAQItem(LOCATION, R.string.faq_location),
        FAQItem(ANONYMOUS, R.string.faq_anonymous),
        FAQItem(NOTIFICATION, R.string.faq_notification),
        FAQItem(BLUETOOTH, R.string.faq_bluetooth),
        FAQItem(POWER_USAGE, R.string.faq_power_usage),
        FAQItem(TECHNICAL, R.string.faq_technical)
    )
)
