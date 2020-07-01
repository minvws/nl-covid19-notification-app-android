/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest

import com.xwray.groupie.Group
import com.xwray.groupie.Section
import nl.rijksoverheid.en.ExposureNotificationsViewModel.NotificationsState
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.items.ButtonItem
import nl.rijksoverheid.en.items.ErrorBoxItem
import nl.rijksoverheid.en.items.HeaderItem
import nl.rijksoverheid.en.items.IllustrationItem
import nl.rijksoverheid.en.items.ParagraphItem
import nl.rijksoverheid.en.labtest.LabTestViewModel.KeyState

class LabTestSection(
    private val retry: () -> Unit,
    private val upload: () -> Unit,
    private val requestConsent: () -> Unit
) : Section() {
    var keyState: KeyState = KeyState.Loading
    var notificationsState: NotificationsState =
        NotificationsState.Enabled

    fun update(keyState: KeyState) {
        this.keyState = keyState
        update()
    }

    fun update(notificationsState: NotificationsState) {
        this.notificationsState = notificationsState
        update()
    }

    private fun update() {
        update(
            mutableListOf<Group>(
                IllustrationItem(R.drawable.illustration_lab_test),
                HeaderItem(R.string.lab_test_header_1),
                ParagraphItem(R.string.lab_test_paragraph_2),
                HeaderItem(R.string.lab_test_header_3),
                LabTestKeyItem(keyState, retry),
                HeaderItem(R.string.lab_test_header_5),
                ParagraphItem(R.string.lab_test_paragraph_6),
                ButtonItem(
                    text = R.string.lab_test_button,
                    buttonClickListener = upload,
                    enabled = keyState is KeyState.Success && notificationsState is NotificationsState.Enabled
                )
            ).apply {
                if (notificationsState !is NotificationsState.Enabled) {
                    add(
                        7,
                        ErrorBoxItem(
                            R.string.error_upload_not_available,
                            R.string.status_en_api_disabled_enable,
                            requestConsent
                        )
                    )
                }
            }
        )
    }
}
