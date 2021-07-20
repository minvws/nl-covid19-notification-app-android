/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest.coronatest

import com.xwray.groupie.Group
import com.xwray.groupie.Section
import nl.rijksoverheid.en.ExposureNotificationsViewModel.NotificationsState
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.items.ButtonItem
import nl.rijksoverheid.en.items.ErrorBoxItem
import nl.rijksoverheid.en.items.IllustrationItem
import nl.rijksoverheid.en.items.ParagraphItem
import nl.rijksoverheid.en.labtest.LabTestViewModel.KeyState
import nl.rijksoverheid.en.labtest.items.LabTestButtonItem
import nl.rijksoverheid.en.labtest.items.LabTestKeyItem
import nl.rijksoverheid.en.labtest.items.LabTestShareKeysItem
import nl.rijksoverheid.en.labtest.items.LabTestStepDescriptionItem
import nl.rijksoverheid.en.labtest.items.LabTestStepItem

class CoronaTestKeySharingSection(
    private val retry: () -> Unit,
    private val requestConsent: () -> Unit,
    private val uploadKeys: () -> Unit,
    private val openCoronaTestWebsite: () -> Unit,
    private val copy: (String) -> Unit,
    private val finish: () -> Unit
) : Section() {
    var keyState: KeyState = KeyState.Loading
        private set
    var notificationsState: NotificationsState = NotificationsState.Enabled
        private set
    var hasSharedKeys: Boolean = false
        private set

    fun update(keyState: KeyState) {
        this.keyState = keyState
        update()
    }

    fun update(notificationsState: NotificationsState) {
        this.notificationsState = notificationsState
        update()
    }

    fun uploadKeysSucceeded() {
        hasSharedKeys = true
        update()
    }

    private fun update() {
        val validKeyState = keyState is KeyState.Success
        val validNotificationsState = notificationsState in listOf(
            NotificationsState.Enabled,
            NotificationsState.BluetoothDisabled,
            NotificationsState.LocationPreconditionNotSatisfied
        )
        val validShareKeysPreconditions = validKeyState && validNotificationsState

        update(
            mutableListOf<Group>(
                IllustrationItem(R.drawable.illustration_lab_test),
                ParagraphItem(R.string.coronatest_description, clickable = true),
                LabTestStepItem(
                    R.string.coronatest_step_1,
                    1,
                    isFirstElement = true,
                    enabled = !hasSharedKeys && validNotificationsState
                ),
                LabTestShareKeysItem(keyState, uploadKeys, retry, hasSharedKeys, validShareKeysPreconditions),
                LabTestStepItem(R.string.coronatest_step_2, 2, enabled = hasSharedKeys),
                LabTestKeyItem(keyState, copy, retry, hasSharedKeys),
                LabTestStepItem(R.string.coronatest_step_3, 3, enabled = hasSharedKeys),
                LabTestButtonItem(R.string.coronatest_webpage_button, openCoronaTestWebsite, hasSharedKeys),
                LabTestStepItem(R.string.coronatest_step_4, 4, isLastElement = true, enabled = hasSharedKeys),
                LabTestStepDescriptionItem(R.string.coronatest_step_4_subtitle, hasSharedKeys),
                ButtonItem(
                    text = R.string.coronatest_finish_button,
                    buttonClickListener = finish,
                    enabled = validKeyState && validNotificationsState && hasSharedKeys
                )
            ).apply {
                if (!validNotificationsState) {
                    add(
                        2, // add box before the key sharing step
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
