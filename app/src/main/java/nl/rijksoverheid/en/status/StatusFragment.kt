/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.ExposureNotificationsViewModel
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentStatusBinding

class StatusFragment : BaseFragment(R.layout.fragment_status) {
    private val statusViewModel: StatusViewModel by viewModels()
    private val viewModel: ExposureNotificationsViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!statusViewModel.isPlayServicesUpToDate()) {
            findNavController().navigate(StatusFragmentDirections.actionUpdatePlayServices())
        } else if (!statusViewModel.hasCompletedOnboarding()) {
            findNavController().navigate(StatusFragmentDirections.actionOnboarding())
        }

        val binding = FragmentStatusBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = statusViewModel

        viewModel.notificationState.observe(viewLifecycleOwner) {
            when (it) {
                ExposureNotificationsViewModel.NotificationsState.Enabled -> { /* all is fine */
                }
                ExposureNotificationsViewModel.NotificationsState.Disabled -> { /* TODO() */
                }
                ExposureNotificationsViewModel.NotificationsState.Unavailable -> showApiUnavailableError()
            }
        }

        viewLifecycleOwner.lifecycle.addObserver(PreconditionsHelper(requireContext()) {
            statusViewModel.refreshStatus()
        })

        binding.infoItem1.container.setOnClickListener {
            findNavController().navigate(R.id.action_about)
        }
    }

    private fun showApiUnavailableError() {
        // TODO
    }
}
