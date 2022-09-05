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
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.ExposureNotificationsViewModel
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentEnableApiBinding
import nl.rijksoverheid.en.lifecyle.observeEvent
import nl.rijksoverheid.en.navigation.navigateCatchingErrors
import nl.rijksoverheid.en.util.ext.setSlideTransition

class EnableApiFragment : BaseFragment(R.layout.fragment_enable_api) {
    private val onboardingViewModel: OnboardingViewModel by activityViewModels()
    private val viewModel: ExposureNotificationsViewModel by activityViewModels()

    private val helper = OnboardingHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSlideTransition()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentEnableApiBinding.bind(view)
        binding.onboardingViewModel = onboardingViewModel
        binding.viewModel = viewModel
        binding.explanationClickListener = View.OnClickListener {
            enterTransition = null
            exitTransition = null
            findNavController().navigateCatchingErrors(EnableApiFragmentDirections.actionExplain())
        }

        helper.observeExposureNotificationsApiEnabled(viewLifecycleOwner)

        onboardingViewModel.skipConsentConfirmation.observeEvent(viewLifecycleOwner) {
            findNavController().navigate(EnableApiFragmentDirections.actionSkipConsentConfirmation())
            findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>(
                SkipConsentConfirmationDialogFragment.SKIP_CONSENT_RESULT
            )?.observe(viewLifecycleOwner) { skip ->
                if (skip) {
                    helper.requestDisableBatteryOptimizations()
                } else {
                    viewModel.requestEnableNotificationsForcingConsent()
                }
            }
        }

        onboardingViewModel.continueOnboarding.observeEvent(viewLifecycleOwner) {
            findNavController().navigateCatchingErrors(
                EnableApiFragmentDirections.actionNext(),
                FragmentNavigatorExtras(
                    binding.appbar to binding.appbar.transitionName
                )
            )
        }
    }
}
