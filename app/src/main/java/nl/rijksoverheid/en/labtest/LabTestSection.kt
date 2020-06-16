/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest

import com.xwray.groupie.Section
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.items.ButtonItem
import nl.rijksoverheid.en.items.HeaderItem
import nl.rijksoverheid.en.items.IllustrationItem
import nl.rijksoverheid.en.items.MessageBoxItem
import nl.rijksoverheid.en.items.ParagraphItem

class LabTestSection : Section(
    listOf(
        IllustrationItem(R.drawable.illustration_lab_test),
        HeaderItem(R.string.lab_test_header_1),
        ParagraphItem(R.string.lab_test_paragraph_2),
        HeaderItem(R.string.lab_test_header_3),
        MessageBoxItem(R.string.lab_test_token_4_error),
        HeaderItem(R.string.lab_test_header_5),
        ParagraphItem(R.string.lab_test_paragraph_6),
        ButtonItem(R.string.lab_test_button) {}
    )
)
