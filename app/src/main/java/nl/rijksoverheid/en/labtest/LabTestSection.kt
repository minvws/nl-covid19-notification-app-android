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
import nl.rijksoverheid.en.ExposureNotificationsViewModel.NotificationsState.Disabled
import nl.rijksoverheid.en.ExposureNotificationsViewModel.NotificationsState.Enabled
import nl.rijksoverheid.en.ExposureNotificationsViewModel.NotificationsState.InvalidPreconditions
import nl.rijksoverheid.en.ExposureNotificationsViewModel.NotificationsState.Unavailable
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.items.ButtonItem
import nl.rijksoverheid.en.items.ErrorBoxItem
import nl.rijksoverheid.en.items.IllustrationItem
import nl.rijksoverheid.en.items.LabTestExplanationItem
import nl.rijksoverheid.en.labtest.LabTestViewModel.KeyState

class LabTestSection(
    private val retry: () -> Unit,
    private val upload: () -> Unit,
    private val requestConsent: () -> Unit
) : Section() {
    private var keyState: KeyState = KeyState.Loading
    private var notificationsState: NotificationsState =
        Enabled

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
                LabTestExplanationItem(R.string.lab_test_paragraph_1),
                LabTestStepItem(R.string.lab_test_step_1, 1, isFirstElement = true),
                LabTestKeyItem(keyState, retry),
                LabTestStepItem(R.string.lab_test_step_2, 2),
                LabTestStepItem(R.string.lab_test_step_3, 3, isLastElement = true),
                ButtonItem(
                    text = R.string.lab_test_button,
                    buttonClickListener = upload,
                    enabled = keyState is KeyState.Success &&
                        (notificationsState is Enabled || notificationsState is InvalidPreconditions)
                )
            ).apply {
                if (notificationsState is Disabled || notificationsState is Unavailable) {
                    add(
                        size - 1, // add box as second to last element
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
