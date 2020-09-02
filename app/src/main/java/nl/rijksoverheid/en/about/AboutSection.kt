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
import nl.rijksoverheid.en.about.FAQItemId.DELETION
import nl.rijksoverheid.en.about.FAQItemId.INTEROPERABILITY
import nl.rijksoverheid.en.about.FAQItemId.LOCATION
import nl.rijksoverheid.en.about.FAQItemId.LOCATION_PERMISSION
import nl.rijksoverheid.en.about.FAQItemId.NOTIFICATION
import nl.rijksoverheid.en.about.FAQItemId.NOTIFICATION_MESSAGE
import nl.rijksoverheid.en.about.FAQItemId.PAUSE
import nl.rijksoverheid.en.about.FAQItemId.POWER_USAGE
import nl.rijksoverheid.en.about.FAQItemId.REASON
import nl.rijksoverheid.en.about.FAQItemId.UPLOAD_KEYS

class AboutSection(showTestPhaseFAQItem: Boolean) : Section(
    listOf(
        FAQOnboardingItem(),
        FAQTechnicalExplanationItem(),
        FAQHeaderItem(R.string.faq_header),
        FAQItem(REASON),
        FAQItem(ANONYMOUS),
        FAQItem(LOCATION),
        FAQItem(NOTIFICATION),
        FAQItem(NOTIFICATION_MESSAGE),
        FAQItem(UPLOAD_KEYS),
        FAQItem(BLUETOOTH),
        FAQItem(LOCATION_PERMISSION),
        FAQItem(POWER_USAGE),
        FAQItem(DELETION),
        FAQItem(PAUSE),
        FAQItem(INTEROPERABILITY)
    ) +
        (if (showTestPhaseFAQItem) listOf(TestPhaseItem()) else emptyList<FAQItem>()) +
        listOf(
            HelpdeskItem(),
            FAQHeaderItem(R.string.about_toolbar_title),
            ReviewItem(),
            PrivacyStatementItem(),
            AccessibilityItem(),
            ColofonItem()
        )
)
