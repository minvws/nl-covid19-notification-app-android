/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.notification

import com.xwray.groupie.Section
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.items.BulletedListItem
import nl.rijksoverheid.en.items.HeaderItem
import nl.rijksoverheid.en.items.IllustrationItem
import nl.rijksoverheid.en.items.MessageBoxItem
import nl.rijksoverheid.en.items.ParagraphItem

class GenericNotificationSection(phoneNumber: String) : Section(
    listOf(
        IllustrationItem(R.drawable.illustration_generic_notification),
        HeaderItem(R.string.generic_notification_header_1),
        ParagraphItem(R.string.generic_notification_paragraph_2),
        HeaderItem(R.string.generic_notification_header_3),
        BulletedListItem(R.string.post_notification_list_4),
        HeaderItem(R.string.post_notification_header_6),
        ParagraphItem(R.string.post_notification_paragraph_7),
        ParagraphItem(R.string.post_notification_paragraph_8),
        ParagraphItem(R.string.post_notification_paragraph_9, phoneNumber),
        MessageBoxItem(R.string.post_notification_message),
        HeaderItem(R.string.treat_perspective_header_1),
        BulletedListItem(R.string.treat_perspective_list_2)
    )
)
