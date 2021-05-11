/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.onboarding

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
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
import nl.rijksoverheid.en.util.launchDisableBatteryOptimizationsRequest

class HowItWorksFragment : BaseFragment(R.layout.fragment_list_with_button) {
    private val viewModel: ExposureNotificationsViewModel by activityViewModels()
    private val onboardingViewModel: OnboardingViewModel by activityViewModels()

    private val adapter = GroupAdapter<GroupieViewHolder>().apply { add(HowItWorksSection()) }

    private val disableBatteryOptimizationsResultRegistration =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { onboardingViewModel.continueOnboarding() }

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
                disableBatteryOptimizationsResultRegistration.launchDisableBatteryOptimizationsRequest { onboardingViewModel.continueOnboarding() }
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
}
