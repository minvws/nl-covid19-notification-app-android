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
import nl.rijksoverheid.en.about.FAQItemId.ANONYMOUS
import nl.rijksoverheid.en.about.FAQItemId.BLUETOOTH
import nl.rijksoverheid.en.about.FAQItemId.LOCATION
import nl.rijksoverheid.en.about.FAQItemId.NOTIFICATION
import nl.rijksoverheid.en.about.FAQItemId.NOTIFICATION_MESSAGE
import nl.rijksoverheid.en.about.FAQItemId.POWER_USAGE
import nl.rijksoverheid.en.about.FAQItemId.REASON

class HowItWorksSection : Section(
    listOf(
        FAQHeaderItem(R.string.faq_header),
        FAQItem(REASON),
        FAQItem(ANONYMOUS),
        FAQItem(LOCATION),
        FAQItem(NOTIFICATION),
        FAQItem(NOTIFICATION_MESSAGE),
        FAQItem(BLUETOOTH),
        FAQItem(POWER_USAGE)
    )
)
