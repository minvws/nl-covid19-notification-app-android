/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.ExposureNotificationsViewModel
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.about.FAQDetailSections
import nl.rijksoverheid.en.about.FAQHeaderItem
import nl.rijksoverheid.en.about.FAQItem
import nl.rijksoverheid.en.about.FAQItemDecoration
import nl.rijksoverheid.en.about.FAQItemId
import nl.rijksoverheid.en.databinding.FragmentListWithButtonBinding
import nl.rijksoverheid.en.lifecyle.observeEvent
import nl.rijksoverheid.en.navigation.navigateCatchingErrors
import nl.rijksoverheid.en.util.ext.setSlideTransition

private val crossLinks = mapOf(
    FAQItemId.REASON to listOf(FAQItemId.LOCATION, FAQItemId.NOTIFICATION_MESSAGE),
    FAQItemId.ANONYMOUS to listOf(
        FAQItemId.NOTIFICATION_MESSAGE,
        FAQItemId.LOCATION,
        FAQItemId.LOCATION_PERMISSION
    ),
    FAQItemId.LOCATION to listOf(FAQItemId.BLUETOOTH, FAQItemId.LOCATION_PERMISSION),
    FAQItemId.NOTIFICATION to listOf(FAQItemId.NOTIFICATION_MESSAGE, FAQItemId.BLUETOOTH),
    FAQItemId.NOTIFICATION_MESSAGE to listOf(
        FAQItemId.NOTIFICATION,
        FAQItemId.REASON,
        FAQItemId.BLUETOOTH
    ),
    FAQItemId.LOCATION_PERMISSION to listOf(
        FAQItemId.LOCATION,
        FAQItemId.REASON,
        FAQItemId.ANONYMOUS
    ),
    FAQItemId.BLUETOOTH to listOf(FAQItemId.NOTIFICATION, FAQItemId.ANONYMOUS),
    FAQItemId.POWER_USAGE to listOf(FAQItemId.LOCATION_PERMISSION, FAQItemId.REASON)
)

class HowItWorksDetailFragment : BaseFragment(R.layout.fragment_list_with_button) {
    private val viewModel: ExposureNotificationsViewModel by activityViewModels()
    private val onboardingViewModel: OnboardingViewModel by activityViewModels()

    private val args: HowItWorksDetailFragmentArgs by navArgs()

    private val adapter = GroupAdapter<GroupieViewHolder>()

    private val helper = OnboardingHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter.add(
            FAQDetailSections(
                openAndroidSettings = {
                    startActivity(Intent(ExposureNotificationClient.ACTION_EXPOSURE_NOTIFICATION_SETTINGS))
                }
            ).getSection(args.faqItemId)
        )

        crossLinks[args.faqItemId]?.let { crossLinks ->
            adapter.add(FAQHeaderItem(R.string.cross_links_header))
            adapter.addAll(crossLinks.map(::FAQItem))
        }

        setSlideTransition()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentListWithButtonBinding.bind(view)

        binding.toolbar.setTitle(R.string.onboarding_how_it_works_detail_toolbar_title)
        binding.content.adapter = adapter
        binding.content.addItemDecoration(
            FAQItemDecoration(
                requireContext(),
                resources.getDimensionPixelOffset(R.dimen.activity_horizontal_margin)
            )
        )

        adapter.setOnItemClickListener { item, _ ->
            when (item) {
                is FAQItem -> {
                    enterTransition = exitTransition
                    findNavController().navigateCatchingErrors(
                        HowItWorksDetailFragmentDirections.actionHowItWorksDetail(item.id),
                        FragmentNavigatorExtras(binding.appbar to binding.appbar.transitionName)
                    )
                }
            }
        }

        binding.button.apply {
            setText(R.string.onboarding_how_it_works_request_consent)
            setOnClickListener { viewModel.requestEnableNotificationsForcingConsent() }
        }

        helper.observeExposureNotificationsApiEnabled(viewLifecycleOwner)

        onboardingViewModel.continueOnboarding.observeEvent(viewLifecycleOwner) {
            findNavController().navigateCatchingErrors(
                HowItWorksDetailFragmentDirections.actionNext(),
                FragmentNavigatorExtras(
                    binding.appbar to binding.appbar.transitionName
                )
            )
        }
    }
}
