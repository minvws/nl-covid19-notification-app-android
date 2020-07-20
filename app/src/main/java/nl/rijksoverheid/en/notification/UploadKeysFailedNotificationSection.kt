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

class UploadKeysFailedNotificationSection : Section(
    listOf(
        IllustrationItem(R.drawable.illustration_upload_keys_failed_notification),
        HeaderItem(R.string.upload_keys_failed_notification_header),
        ParagraphItem(R.string.upload_keys_failed_notification_paragraph)
    )
)
