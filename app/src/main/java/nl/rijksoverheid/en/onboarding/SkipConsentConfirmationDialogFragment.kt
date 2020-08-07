/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.onboarding

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nl.rijksoverheid.en.R

class SkipConsentConfirmationDialogFragment : DialogFragment() {

    companion object {
        const val SKIP_CONSENT_RESULT = "skip_consent"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.onboarding_consent_skip_dialog_title)
            .setMessage(R.string.onboarding_consent_skip_dialog_message)
            .setPositiveButton(R.string.onboarding_consent_skip_dialog_enable) { _, _ ->
                findNavController().currentBackStackEntry?.savedStateHandle?.set(
                    SKIP_CONSENT_RESULT, false
                )
            }
            .setNegativeButton(R.string.onboarding_consent_skip_dialog_skip) { _, _ ->
                findNavController().currentBackStackEntry?.savedStateHandle?.set(
                    SKIP_CONSENT_RESULT, true
                )
            }
        return builder.create()
    }
}
