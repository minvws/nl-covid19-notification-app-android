/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.navigation.NavDirections
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.about.FAQItemId.ANONYMOUS
import nl.rijksoverheid.en.about.FAQItemId.BLUETOOTH
import nl.rijksoverheid.en.about.FAQItemId.DELETION
import nl.rijksoverheid.en.about.FAQItemId.INTEROPERABILITY
import nl.rijksoverheid.en.about.FAQItemId.INTEROP_COUNTRIES
import nl.rijksoverheid.en.about.FAQItemId.LOCATION
import nl.rijksoverheid.en.about.FAQItemId.LOCATION_PERMISSION
import nl.rijksoverheid.en.about.FAQItemId.NOTIFICATION
import nl.rijksoverheid.en.about.FAQItemId.NOTIFICATION_MESSAGE
import nl.rijksoverheid.en.about.FAQItemId.ONBOARDING
import nl.rijksoverheid.en.about.FAQItemId.PAUSE
import nl.rijksoverheid.en.about.FAQItemId.POWER_USAGE
import nl.rijksoverheid.en.about.FAQItemId.REASON
import nl.rijksoverheid.en.about.FAQItemId.STILL_USEFUL
import nl.rijksoverheid.en.about.FAQItemId.TECHNICAL
import nl.rijksoverheid.en.about.FAQItemId.UPLOAD_KEYS
import nl.rijksoverheid.en.about.FAQItemId.UPLOAD_KEYS_GENERIC
import nl.rijksoverheid.en.databinding.FragmentListBinding
import nl.rijksoverheid.en.navigation.navigateCatchingErrors
import nl.rijksoverheid.en.util.ext.setSlideTransition

// Map of linked FAQ pages for each FAQ page
private val crossLinks = mapOf(
    STILL_USEFUL to listOf(REASON, TECHNICAL),
    ONBOARDING to listOf(REASON, LOCATION, ANONYMOUS),
    TECHNICAL to listOf(BLUETOOTH, DELETION, INTEROPERABILITY),
    REASON to listOf(TECHNICAL, NOTIFICATION_MESSAGE),
    ANONYMOUS to listOf(TECHNICAL, NOTIFICATION_MESSAGE, LOCATION, LOCATION_PERMISSION),
    LOCATION to listOf(BLUETOOTH, LOCATION_PERMISSION),
    NOTIFICATION to listOf(NOTIFICATION_MESSAGE, BLUETOOTH, UPLOAD_KEYS),
    UPLOAD_KEYS to listOf(NOTIFICATION_MESSAGE, ANONYMOUS, TECHNICAL),
    UPLOAD_KEYS_GENERIC to listOf(NOTIFICATION_MESSAGE, ANONYMOUS, TECHNICAL),
    NOTIFICATION_MESSAGE to listOf(NOTIFICATION, BLUETOOTH, REASON),
    BLUETOOTH to listOf(NOTIFICATION, ANONYMOUS),
    LOCATION_PERMISSION to listOf(LOCATION, REASON, ANONYMOUS),
    POWER_USAGE to listOf(LOCATION_PERMISSION, REASON, PAUSE),
    DELETION to listOf(BLUETOOTH, PAUSE),
    PAUSE to listOf(BLUETOOTH, POWER_USAGE, LOCATION),
    INTEROPERABILITY to listOf(INTEROP_COUNTRIES, TECHNICAL, NOTIFICATION, LOCATION)
)

/**
 * Fragment for displaying a FAQ page including links to subpages based on the crossLinks above
 */
class AboutDetailFragment : BaseFragment(R.layout.fragment_list) {

    private val args: AboutDetailFragmentArgs by navArgs()

    private val adapter = GroupAdapter<GroupieViewHolder>()

    private lateinit var binding: FragmentListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter.add(
            FAQDetailSections(
                openAndroidSettings = {
                    startActivity(Intent(ExposureNotificationClient.ACTION_EXPOSURE_NOTIFICATION_SETTINGS))
                },
                openAppSettings = {
                    findNavController().navigateCatchingErrors(AboutDetailFragmentDirections.actionSettings())
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

        binding = FragmentListBinding.bind(view)

        binding.toolbar.setTitle(
            when (args.faqItemId) {
                ONBOARDING -> R.string.about_onboarding_title
                TECHNICAL -> R.string.faq_technical_toolbar_title
                else -> R.string.faq_detail_toolbar_title
            }
        )
        binding.content.adapter = adapter
        binding.content.addItemDecoration(
            FAQItemDecoration(
                requireContext(),
                resources.getDimensionPixelOffset(R.dimen.activity_horizontal_margin)
            )
        )

        adapter.setOnItemClickListener { item, _ ->
            when (item) {
                is GithubItem -> openUri(getString(R.string.github_url))
                is FAQItem -> {
                    if (item.id == INTEROP_COUNTRIES) {
                        openUri(getString(R.string.interop_countries_url, getString(R.string.app_language)))
                    } else {
                        navigateWithTransition(AboutDetailFragmentDirections.actionAboutDetail(item.id))
                    }
                }
                is FAQOnboardingItem -> {
                    navigateWithTransition(AboutDetailFragmentDirections.actionAboutDetail(ONBOARDING))
                }
                is FAQTechnicalExplanationItem -> {
                    navigateWithTransition(AboutDetailFragmentDirections.actionAboutDetail(TECHNICAL))
                }
            }
        }
    }

    private fun navigateWithTransition(directions: NavDirections) {
        enterTransition = exitTransition
        findNavController().navigateCatchingErrors(
            directions,
            FragmentNavigatorExtras(binding.appbar to binding.appbar.transitionName)
        )
    }

    private fun openUri(uriString: String) {
        val uri = Uri.parse(uriString)
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}
