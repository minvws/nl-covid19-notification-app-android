/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.onboarding

import com.xwray.groupie.Section
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.about.FAQHeaderItem
import nl.rijksoverheid.en.about.FAQItem
import nl.rijksoverheid.en.about.FAQItemId

class HowItWorksSection : Section(
    listOf(
        FAQHeaderItem(R.string.faq_header),
        FAQItem(FAQItemId.LOCATION, R.string.faq_location),
        FAQItem(FAQItemId.ANONYMOUS, R.string.faq_anonymous),
        FAQItem(FAQItemId.NOTIFICATION, R.string.faq_notification),
        FAQItem(FAQItemId.BLUETOOTH, R.string.faq_bluetooth),
        FAQItem(FAQItemId.POWER_USAGE, R.string.faq_power_usage)
    )
)
