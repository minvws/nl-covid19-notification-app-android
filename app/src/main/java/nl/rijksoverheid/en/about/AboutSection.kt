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
import nl.rijksoverheid.en.about.FAQItemId.LOCATION
import nl.rijksoverheid.en.about.FAQItemId.LOCATION_PERMISSION
import nl.rijksoverheid.en.about.FAQItemId.NOTIFICATION
import nl.rijksoverheid.en.about.FAQItemId.POWER_USAGE
import nl.rijksoverheid.en.about.FAQItemId.REASON
import nl.rijksoverheid.en.about.FAQItemId.UPLOAD_KEYS

class AboutSection : Section(
    listOf(
        FAQOnboardingItem(),
        FAQTechnicalExplanationItem(),
        FAQHeaderItem(R.string.faq_header),
        FAQItem(REASON, R.string.faq_reason),
        FAQItem(LOCATION, R.string.faq_location),
        FAQItem(ANONYMOUS, R.string.faq_anonymous),
        FAQItem(NOTIFICATION, R.string.faq_notification),
        FAQItem(UPLOAD_KEYS, R.string.faq_upload_keys),
        FAQItem(BLUETOOTH, R.string.faq_bluetooth),
        FAQItem(LOCATION_PERMISSION, R.string.faq_location_permission),
        FAQItem(POWER_USAGE, R.string.faq_power_usage),
        FAQItem(DELETION, R.string.faq_deletion),
        HelpdeskItem(),
        FAQHeaderItem(R.string.about_toolbar_title),
        ReviewItem(),
        PrivacyStatementItem(),
        AccessibilityItem(),
        ColofonItem()
    )
)
