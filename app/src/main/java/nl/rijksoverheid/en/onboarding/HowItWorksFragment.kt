/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.onboarding

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.ExposureNotificationsViewModel
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.about.FAQItem
import nl.rijksoverheid.en.about.FAQItemDecoration
import nl.rijksoverheid.en.databinding.FragmentListWithButtonBinding
import nl.rijksoverheid.en.ignoreInitiallyEnabled
import nl.rijksoverheid.en.lifecyle.EventObserver
import nl.rijksoverheid.en.navigation.navigateCatchingErrors
import nl.rijksoverheid.en.util.requestDisableBatteryOptimizations
import timber.log.Timber

private const val RC_DISABLE_BATTERY_OPTIMIZATIONS = 1

class HowItWorksFragment : BaseFragment(R.layout.fragment_list_with_button) {
    private val viewModel: ExposureNotificationsViewModel by activityViewModels()
    private val onboardingViewModel: OnboardingViewModel by viewModels()

    private val adapter = GroupAdapter<GroupieViewHolder>().apply { add(HowItWorksSection()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition =
            TransitionInflater.from(context).inflateTransition(R.transition.slide_start)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentListWithButtonBinding.bind(view)

        binding.toolbar.apply {
            setTitle(R.string.onboarding_how_it_works_toolbar_title)
            setNavigationIcon(R.drawable.ic_close)
            setNavigationContentDescription(R.string.cd_close)
        }
        binding.content.addItemDecoration(
            FAQItemDecoration(
                requireContext(),
                resources.getDimensionPixelOffset(R.dimen.activity_horizontal_margin)
            )
        )
        binding.content.adapter = adapter

        adapter.setOnItemClickListener { item, _ ->
            if (item is FAQItem) {
                findNavController().navigateCatchingErrors(
                    HowItWorksFragmentDirections.actionHowItWorksDetail(item.id),
                    FragmentNavigatorExtras(binding.appbar to binding.appbar.transitionName)
                )
            }
        }

        binding.button.apply {
            setText(R.string.onboarding_how_it_works_request_consent)
            setOnClickListener { viewModel.requestEnableNotificationsForcingConsent() }
        }

        viewModel.notificationState.ignoreInitiallyEnabled().observe(viewLifecycleOwner) {
            if (it is ExposureNotificationsViewModel.NotificationsState.Enabled) {
                requestDisableBatteryOptimizationsAndContinue()
            }
        }

        onboardingViewModel.continueOnboarding.observe(
            viewLifecycleOwner,
            EventObserver {
                findNavController().navigateCatchingErrors(
                    HowItWorksFragmentDirections.actionNext(),
                    FragmentNavigatorExtras(
                        binding.appbar to binding.appbar.transitionName
                    )
                )
            }
        )
    }

    private fun requestDisableBatteryOptimizationsAndContinue() {
        try {
            requestDisableBatteryOptimizations(RC_DISABLE_BATTERY_OPTIMIZATIONS)
        } catch (ex: ActivityNotFoundException) {
            // ignore
            Timber.e(ex)
            onboardingViewModel.continueOnboarding()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_DISABLE_BATTERY_OPTIMIZATIONS) {
            onboardingViewModel.continueOnboarding()
        }
    }
}
