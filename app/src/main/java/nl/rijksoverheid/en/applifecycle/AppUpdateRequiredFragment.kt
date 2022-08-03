/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.applifecycle

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentAppUpdateRequiredBinding
import nl.rijksoverheid.en.util.IntentHelper

class AppUpdateRequiredFragment : BaseFragment(R.layout.fragment_app_update_required) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentAppUpdateRequiredBinding.bind(view)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            requireActivity().finish()
        }

        binding.next.setOnClickListener {
            IntentHelper.openPlayStore(requireActivity(), BuildConfig.APPLICATION_ID)
        }
    }
}
