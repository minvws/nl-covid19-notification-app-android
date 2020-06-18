/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nl.rijksoverheid.en.R

class RemoveExposedMessageDialogFragment : DialogFragment() {
    private val statusViewModel: StatusViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.status_dialog_remove_exposure_title)
            .setMessage(R.string.status_dialog_remove_exposure_message)
            .setPositiveButton(R.string.status_dialog_remove_exposure_confirm) { _, _ ->
                statusViewModel.removeExposure()
            }
            .setNegativeButton(R.string.status_dialog_remove_exposure_cancel, null)
        return builder.create()
    }
}
