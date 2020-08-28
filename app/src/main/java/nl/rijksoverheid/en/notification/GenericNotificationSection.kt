/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.notification

import com.xwray.groupie.Section
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.items.HeaderItem
import nl.rijksoverheid.en.items.IllustrationItem
import nl.rijksoverheid.en.items.ParagraphItem

class GenericNotificationSection : Section(
    listOf(
        IllustrationItem(R.drawable.illustration_generic_notification),
        HeaderItem(R.string.generic_notification_header_1),
        ParagraphItem(R.string.generic_notification_paragraph_2),
        HeaderItem(R.string.generic_notification_header_3),
        ParagraphItem(R.string.generic_notification_paragraph_4),
        ParagraphItem(R.string.generic_notification_paragraph_5),
        HeaderItem(R.string.generic_notification_header_8),
        ParagraphItem(R.string.generic_notification_paragraph_9),
        ParagraphItem(R.string.generic_notification_paragraph_10),
        HeaderItem(R.string.generic_notification_header_11),
        ParagraphItem(R.string.generic_notification_paragraph_12),
        ParagraphItem(R.string.generic_notification_paragraph_13)
    )
)
