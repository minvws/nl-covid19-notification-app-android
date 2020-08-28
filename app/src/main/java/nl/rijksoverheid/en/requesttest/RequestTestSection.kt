/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.requesttest

import com.xwray.groupie.Section
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.items.BulletedListItem
import nl.rijksoverheid.en.items.HeaderItem
import nl.rijksoverheid.en.items.IllustrationItem
import nl.rijksoverheid.en.items.ParagraphItem

class RequestTestSection : Section(
    listOf(
        IllustrationItem(R.drawable.illustration_request_test),
        HeaderItem(R.string.request_test_header_1),
        ParagraphItem(R.string.request_test_paragraph_2),
        ParagraphItem(R.string.request_test_paragraph_3),
        HeaderItem(R.string.treat_perspective_header_1),
        BulletedListItem(R.string.treat_perspective_list_2),
        ParagraphItem(R.string.request_test_paragraph_4),
        HeaderItem(R.string.request_test_header_5),
        ParagraphItem(R.string.post_notification_message)
    )
)
