/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.phonecall

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.util.forceLtr

/**
 * DialogFragment for providing information regarding a device which doesn't support phone calls.
 */
class PhoneCallNotSupportedDialogFragment : DialogFragment() {
    private val args: PhoneCallNotSupportedDialogFragmentArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.phone_call_not_supported_dialog_title)
            .setMessage(getString(R.string.phone_call_not_supported_dialog_message, args.phoneNumber.forceLtr()))
            .setPositiveButton(R.string.phone_call_not_supported_dialog_close, null)
        return builder.create()
    }
}
