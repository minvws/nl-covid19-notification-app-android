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
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionInflater
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentListBinding

class AboutDetailFragment : BaseFragment(R.layout.fragment_list) {

    private val args: AboutDetailFragmentArgs by navArgs()

    private val adapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter.add(
            FAQDetailSections(
                openSettings = {
                    startActivity(Intent(ExposureNotificationClient.ACTION_EXPOSURE_NOTIFICATION_SETTINGS))
                }
            ).getSection(args.faqItemId)
        )

        enterTransition = TransitionInflater.from(context).inflateTransition(R.transition.slide_end)
        exitTransition =
            TransitionInflater.from(context).inflateTransition(R.transition.slide_start)

        sharedElementEnterTransition =
            TransitionInflater.from(context).inflateTransition(R.transition.move_fade)
        sharedElementReturnTransition = sharedElementEnterTransition
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentListBinding.bind(view)

        binding.toolbar.setTitle(
            when (args.faqItemId) {
                FAQItemId.ONBOARDING -> R.string.about_onboarding_title
                FAQItemId.TECHNICAL -> R.string.faq_technical_toolbar_title
                else -> R.string.faq_detail_toolbar_title
            }
        )
        binding.content.adapter = adapter

        adapter.setOnItemClickListener { item, _ ->
            when (item) {
                is GithubItem -> {
                    val uri = Uri.parse(getString(R.string.github_url))
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
            }
        }
    }
}
