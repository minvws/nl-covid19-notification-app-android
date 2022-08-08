/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest.coronatest

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nl.rijksoverheid.en.R

class FinishKeySharingDialogFragment : DialogFragment() {

    companion object {
        const val FINISH_KEY_SHARING_RESULT = "finish_key_sharing"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.coronatest_finish_confirmation_title))
            .setMessage(getString(R.string.coronatest_finish_confirmation_text))
            .setPositiveButton(R.string.coronatest_finish_confirmation_accept) { _, _ ->
                findNavController().currentBackStackEntry?.savedStateHandle?.set(
                    FINISH_KEY_SHARING_RESULT,
                    true
                )
            }
            .setNegativeButton(R.string.coronatest_finish_confirmation_cancel, null)
        return builder.create()
    }
}
