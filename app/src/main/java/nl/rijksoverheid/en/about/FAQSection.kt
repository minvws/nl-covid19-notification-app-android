/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.about

import com.xwray.groupie.Section
import nl.rijksoverheid.en.R

class FAQSection : Section(
    listOf(
        FAQHeaderItem(R.string.faq_header),
        FAQItem(R.string.faq_1),
        FAQItem(R.string.faq_2),
        FAQItem(R.string.faq_3),
        FAQItem(R.string.faq_4),
        FAQItem(R.string.faq_5)
    )
)
