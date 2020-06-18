/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import android.app.Activity
import android.content.Intent
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
import nl.rijksoverheid.en.lifecyle.EventObserver

val RC_CONFIRM_DELETE_EXPOSURE = 1
private const val TAG_CONFIRM_DELETE_EXPOSURE = "confirm_delete_exposure"

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

        binding.infoItem1.root.setOnClickListener {
            findNavController().navigate(StatusFragmentDirections.actionAbout())
        }
        binding.infoItem2.root.setOnClickListener {
            findNavController().navigate(StatusFragmentDirections.actionGenericNotification())
        }
        binding.infoItem3.root.setOnClickListener {
            findNavController().navigate(StatusFragmentDirections.actionRequestTest())
        }
        binding.infoItem4.root.setOnClickListener {
            findNavController().navigate(StatusFragmentDirections.actionLabTest())
        }

        viewModel.notificationState.observe(viewLifecycleOwner) {
            when (it) {
                ExposureNotificationsViewModel.NotificationsState.Enabled -> {
                    statusViewModel.refreshStatus()
                }
                ExposureNotificationsViewModel.NotificationsState.Disabled -> {
                    statusViewModel.refreshStatus()
                }
                ExposureNotificationsViewModel.NotificationsState.Unavailable -> showApiUnavailableError()
            }
        }

        statusViewModel.requestEnableNotifications.observe(viewLifecycleOwner, EventObserver {
            viewModel.requestEnableNotifications()
        })

        statusViewModel.confirmRemoveExposedMessage.observe(viewLifecycleOwner, EventObserver {
            if (parentFragmentManager.findFragmentByTag(TAG_CONFIRM_DELETE_EXPOSURE) == null) {
                RemoveExposedMessageDialogFragment().apply {
                    setTargetFragment(this@StatusFragment, RC_CONFIRM_DELETE_EXPOSURE)
                }.show(
                    parentFragmentManager,
                    TAG_CONFIRM_DELETE_EXPOSURE
                )
            }
        })

        statusViewModel.navigateToPostNotification.observe(viewLifecycleOwner, EventObserver {
            findNavController().navigate(StatusFragmentDirections.actionPostNotification(it.toEpochDay()))
        })

        viewLifecycleOwner.lifecycle.addObserver(PreconditionsHelper(requireContext()) {
            statusViewModel.refreshStatus()
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_CONFIRM_DELETE_EXPOSURE && resultCode == Activity.RESULT_OK) {
            statusViewModel.removeExposure()
        }
    }

    private fun showApiUnavailableError() {
        // TODO
    }
}
