/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.applifecycle

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentNoInternetBinding

class NoInternetFragment : Fragment(R.layout.fragment_no_internet) {

    private val appLifecycleViewModel: AppLifecycleViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentNoInternetBinding.bind(view)

        binding.retryButton.setOnClickListener {
            appLifecycleViewModel.checkForForcedAppUpdate()
        }

        appLifecycleViewModel.updateEvent.observe(viewLifecycleOwner) {
            if (it is AppLifecycleViewModel.AppLifecycleStatus.Ready)
                findNavController().popBackStack()
        }
    }
}
