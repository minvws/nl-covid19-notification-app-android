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
import nl.rijksoverheid.en.items.ParagraphItem

class PostNotificationSection(
    daysSince: String,
    date: String,
    stayHomeDate: String
) : Section(
    listOf(
        IllustrationItem(R.drawable.illustration_post_notification),
        HeaderItem(R.string.post_notification_header_1),
        ParagraphItem(R.string.post_notification_paragraph_2, daysSince, date),
        HeaderItem(R.string.post_notification_header_3),
        BulletedListItem(R.string.post_notification_list_4, stayHomeDate),
        ParagraphItem(R.string.post_notification_guidance_follow_ggd),
        HeaderItem(R.string.post_notification_stay_home_header, stayHomeDate),
        BulletedListItem(R.string.post_notification_stay_home_list),
        HeaderItem(R.string.post_notification_no_visitors_header),
        BulletedListItem(R.string.post_notification_no_visitors_list),
        HeaderItem(R.string.post_notification_medical_help_header),
        BulletedListItem(R.string.post_notification_medical_help_list),
        HeaderItem(R.string.post_notification_after_stay_home_header, stayHomeDate),
        ParagraphItem(R.string.post_notification_after_stay_home_paragraph, stayHomeDate),
        HeaderItem(R.string.treat_perspective_header_1),
        BulletedListItem(R.string.treat_perspective_list_2)
    )
)
