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
import nl.rijksoverheid.en.labtest.items.LabTestUsedKeyItem

class LabTestDoneSection(
    generatedKey: String,
    hasIndependentKeySharing: Boolean,
    close: () -> Unit
) : Section(

    if (hasIndependentKeySharing) {
        listOf(
            IllustrationItem(R.drawable.illustration_lab_test_done),
            HeaderItem(R.string.lab_test_done_generic_header_1),
            ParagraphItem(R.string.lab_test_done_generic_paragraph_2),
            LabTestUsedKeyItem(generatedKey),
            MessageBoxItem(R.string.lab_test_done_generic_box_4),
            ButtonItem(R.string.lab_test_done_button, close)
        )
    } else {
        listOf(
            IllustrationItem(R.drawable.illustration_lab_test_done),
            HeaderItem(R.string.lab_test_done_header_1),
            ParagraphItem(R.string.lab_test_done_paragraph_2),
            LabTestUsedKeyItem(generatedKey),
            MessageBoxItem(R.string.lab_test_done_box_4),
            ButtonItem(R.string.lab_test_done_button, close)
        )
    }
)
