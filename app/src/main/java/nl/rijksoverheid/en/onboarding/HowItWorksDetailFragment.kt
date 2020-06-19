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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionInflater
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.ExposureNotificationsViewModel
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.about.FAQDetailSections
import nl.rijksoverheid.en.databinding.FragmentHowItWorksBinding

class HowItWorksDetailFragment : BaseFragment(R.layout.fragment_how_it_works) {
    private val onboardingViewModel: OnboardingViewModel by viewModels()
    private val viewModel: ExposureNotificationsViewModel by activityViewModels()

    private val args: HowItWorksDetailFragmentArgs by navArgs()

    private val adapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter.add(FAQDetailSections().getSection(args.faqItemId))

        enterTransition = TransitionInflater.from(context).inflateTransition(R.transition.slide_end)
        exitTransition =
            TransitionInflater.from(context).inflateTransition(R.transition.slide_start)

        sharedElementEnterTransition =
            TransitionInflater.from(context).inflateTransition(R.transition.move_fade)
        sharedElementReturnTransition = sharedElementEnterTransition
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentHowItWorksBinding.bind(view)

        binding.toolbar.setTitle(R.string.onboarding_how_it_works_detail_toolbar_title)
        binding.content.adapter = adapter

        binding.request.setOnClickListener { viewModel.requestEnableNotifications() }

        viewModel.notificationState.observe(viewLifecycleOwner) {
            if (it is ExposureNotificationsViewModel.NotificationsState.Enabled) {
                onboardingViewModel.finishOnboarding()
            }
        }

        onboardingViewModel.onboardingComplete.observe(viewLifecycleOwner) {
            enterTransition = null
            exitTransition = null
            findNavController().popBackStack(R.id.nav_onboarding, true)
        }
    }
}
