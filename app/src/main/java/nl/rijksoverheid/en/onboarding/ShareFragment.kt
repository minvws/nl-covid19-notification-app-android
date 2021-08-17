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
import androidx.activity.addCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentShareBinding
import nl.rijksoverheid.en.util.ext.setSlideTransition

class ShareFragment : BaseFragment(R.layout.fragment_share) {
    private val onboardingViewModel: OnboardingViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSlideTransition()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentShareBinding.bind(view)

        binding.toolbar.apply {
            setNavigationOnClickListener {
                onboardingViewModel.finishOnboarding()
            }
            setNavigationIcon(R.drawable.ic_close)
            setNavigationContentDescription(R.string.cd_close)
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            onboardingViewModel.finishOnboarding()
        }

        binding.nextButtonClickListener =
            View.OnClickListener { onboardingViewModel.finishOnboarding() }
        binding.shareButtonClickListener = View.OnClickListener { share() }

        onboardingViewModel.onboardingComplete.observe(viewLifecycleOwner) {
            enterTransition = null
            exitTransition = null
            findNavController().popBackStack(R.id.nav_onboarding, true)
        }
    }

    private fun share() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                getString(R.string.share_content, getString(R.string.share_url))
            )
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, getString(R.string.share_title))
        startActivity(shareIntent)
    }
}
