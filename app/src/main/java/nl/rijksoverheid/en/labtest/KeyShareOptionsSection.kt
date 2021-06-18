/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest

import com.xwray.groupie.Section
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.items.HeaderItem
import nl.rijksoverheid.en.items.ParagraphItem

class KeyShareOptionsSection : Section(
    listOf(
        HeaderItem(R.string.post_keys_options_header),
        ParagraphItem(R.string.post_keys_options_description),
        KeyShareOptionItem.CoronaTest,
        KeyShareOptionItem.GGD
    )
)
