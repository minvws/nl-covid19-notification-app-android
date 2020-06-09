/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.onboarding

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.ExposureNotificationsViewModel
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentEnableApiBinding
import nl.rijksoverheid.en.lifecyle.EventObserver
import timber.log.Timber

private const val RC_REQUEST_PERMISSION = 1

class EnableApiFragment : BaseFragment(R.layout.fragment_enable_api) {
    private val viewModel: ExposureNotificationsViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = TransitionInflater.from(context).inflateTransition(R.transition.slide_end)
        exitTransition =
            TransitionInflater.from(context).inflateTransition(R.transition.slide_start)

        sharedElementEnterTransition =
            TransitionInflater.from(context).inflateTransition(R.transition.move_fade)
        sharedElementReturnTransition = sharedElementEnterTransition
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentEnableApiBinding.bind(view)

        binding.explanation.setOnClickListener {
            findNavController().navigate(EnableApiFragmentDirections.actionExplain())
        }
        binding.skip.setOnClickListener {
            findNavController().popBackStack(R.id.nav_onboarding, true)
        }
        binding.request.setOnClickListener {
            viewModel.requestEnableNotifications()
        }

        viewModel.notificationsResult.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                is ExposureNotificationsViewModel.NotificationsStatusResult.ConsentRequired -> {
                    try {
                        requireActivity().startIntentSenderFromFragment(
                            this,
                            it.intent.intentSender,
                            RC_REQUEST_PERMISSION,
                            null,
                            0,
                            0,
                            0,
                            null
                        )
                    } catch (ex: IntentSender.SendIntentException) {
                        Timber.e(ex, "Error requesting consent")
                    }
                }
                is ExposureNotificationsViewModel.NotificationsStatusResult.UnknownError -> TODO()
            }
        })

        viewModel.notificationState.observe(viewLifecycleOwner) {
            when (it) {
                ExposureNotificationsViewModel.NotificationsState.Enabled -> findNavController().popBackStack(
                    R.id.nav_onboarding,
                    true
                )
                ExposureNotificationsViewModel.NotificationsState.Unavailable -> {
                    // TODO device is not supported
                }
                ExposureNotificationsViewModel.NotificationsState.Disabled -> {
                    // user needs to enable
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_REQUEST_PERMISSION && resultCode == Activity.RESULT_OK) {
            // consent was given, request again
            viewModel.requestEnableNotifications()
        }
    }
}
