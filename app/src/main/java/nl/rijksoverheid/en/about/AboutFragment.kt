/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.about

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentListBinding
import nl.rijksoverheid.en.navigation.navigateCatchingErrors
import nl.rijksoverheid.en.util.IntentHelper

class AboutFragment : BaseFragment(R.layout.fragment_list) {
    private val adapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition =
            TransitionInflater.from(context).inflateTransition(R.transition.slide_start)

        adapter.add(AboutSection())
    }

    @SuppressLint("StringFormatInvalid") // for overridden urls in fy language
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentListBinding.bind(view)

        binding.toolbar.setTitle(R.string.about_toolbar_title)

        binding.content.adapter = adapter
        binding.content.addItemDecoration(
            FAQItemDecoration(
                requireContext(),
                resources.getDimensionPixelOffset(R.dimen.activity_horizontal_margin)
            )
        )

        adapter.setOnItemClickListener { item, _ ->
            when (item) {
                is FAQOnboardingItem -> findNavController().navigateCatchingErrors(
                    AboutFragmentDirections.actionAboutDetail(FAQItemId.ONBOARDING),
                    FragmentNavigatorExtras(binding.appbar to binding.appbar.transitionName)
                )
                is FAQTechnicalExplanationItem -> findNavController().navigateCatchingErrors(
                    AboutFragmentDirections.actionAboutDetail(FAQItemId.TECHNICAL),
                    FragmentNavigatorExtras(binding.appbar to binding.appbar.transitionName)
                )
                is FAQItem -> findNavController().navigateCatchingErrors(
                    AboutFragmentDirections.actionAboutDetail(item.id),
                    FragmentNavigatorExtras(binding.appbar to binding.appbar.transitionName)
                )
                is WebsiteLinkItem -> openUrlResWithLanguagePlaceholder(R.string.coronamelder_url)
                is HelpdeskItem -> requestToCallHelpdesk()
                is ReviewItem -> openAppReviewActivity()
                is PrivacyStatementItem -> openUrlResWithLanguagePlaceholder(R.string.privacy_policy_url)
                is AccessibilityItem -> openUrlResWithLanguagePlaceholder(R.string.accessibility_url)
                is ColofonItem -> openUrlResWithLanguagePlaceholder(R.string.colofon_url)
            }
        }
    }

    private fun openUrlResWithLanguagePlaceholder(@StringRes stringRes: Int) {
        val url = Uri.parse(
            getString(stringRes, getString(R.string.app_language))
        )
        CustomTabsIntent.Builder().build().launchUrl(requireContext(), url)
    }

    private fun openAppReviewActivity() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(getString(R.string.play_store_url))
            setPackage("com.android.vending")
        }
        startActivity(intent)
    }

    private fun requestToCallHelpdesk() {
        val phoneNumber = getString(R.string.helpdesk_phone_number)
        IntentHelper.openPhoneCallIntent(
            this,
            phoneNumber,
            AboutFragmentDirections.actionPhoneCallNotSupportedDialog(phoneNumber)
        )
    }
}
