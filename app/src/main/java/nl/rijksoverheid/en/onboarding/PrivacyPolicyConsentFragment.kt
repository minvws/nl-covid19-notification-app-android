/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.onboarding

import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.TextAppearanceSpan
import android.text.style.URLSpan
import android.view.View
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.text.HtmlCompat
import androidx.core.text.getSpans
import androidx.core.view.ViewCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentPrivacyPolicyConsentBinding

class PrivacyPolicyConsentFragment : BaseFragment(R.layout.fragment_privacy_policy_consent) {
    private val onboardingViewModel: OnboardingViewModel by viewModels()

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

        val binding = FragmentPrivacyPolicyConsentBinding.bind(view)
        binding.onboardingViewModel = onboardingViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.nextButtonClickListener = View.OnClickListener {
            findNavController().navigate(
                PrivacyPolicyConsentFragmentDirections.actionNext(), FragmentNavigatorExtras(
                    binding.appbar to binding.appbar.transitionName
                )
            )
        }

        ViewCompat.enableAccessibleClickableSpanSupport(binding.description)
        binding.description.text = SpannableString(
            HtmlCompat.fromHtml(
                view.context.getString(R.string.onboarding_privacy_policy_paragraph),
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
        ).apply {
            getSpans<URLSpan>().forEach {
                val start = getSpanStart(it)
                val end = getSpanEnd(it)
                setSpan(
                    TextAppearanceSpan(view.context, R.style.TextAppearance_App_Link),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        binding.description.setOnClickListener {
            val url = Uri.parse(getString(R.string.privacy_policy_url))
            CustomTabsIntent.Builder().build().launchUrl(requireContext(), url)
        }
    }
}
