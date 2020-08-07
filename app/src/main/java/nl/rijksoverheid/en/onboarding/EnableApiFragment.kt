/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.ExposureNotificationsViewModel
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentEnableApiBinding
import nl.rijksoverheid.en.ignoreInitiallyEnabled
import nl.rijksoverheid.en.lifecyle.EventObserver

class EnableApiFragment : BaseFragment(R.layout.fragment_enable_api) {
    private val onboardingViewModel: OnboardingViewModel by viewModels()
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
        binding.onboardingViewModel = onboardingViewModel
        binding.viewModel = viewModel
        binding.explanationClickListener = View.OnClickListener {
            enterTransition = null
            exitTransition = null
            findNavController().navigate(EnableApiFragmentDirections.actionExplain())
        }

        viewModel.notificationState.ignoreInitiallyEnabled().observe(viewLifecycleOwner) {
            if (it is ExposureNotificationsViewModel.NotificationsState.Enabled) {
                findNavController().navigate(
                    EnableApiFragmentDirections.actionNext(),
                    FragmentNavigatorExtras(
                        binding.appbar to binding.appbar.transitionName
                    )
                )
            }
        }

        onboardingViewModel.skipConsentConfirmation.observe(
            viewLifecycleOwner,
            EventObserver {
                findNavController().navigate(EnableApiFragmentDirections.actionSkipConsentConfirmation())
                findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>(
                    SkipConsentConfirmationDialogFragment.SKIP_CONSENT_RESULT
                )?.observe(viewLifecycleOwner) { skip ->
                    if (skip) {
                        findNavController().navigate(
                            EnableApiFragmentDirections.actionNext(),
                            FragmentNavigatorExtras(
                                binding.appbar to binding.appbar.transitionName
                            )
                        )
                    } else {
                        viewModel.requestEnableNotificationsForcingConsent()
                    }
                }
            }
        )
    }
}
