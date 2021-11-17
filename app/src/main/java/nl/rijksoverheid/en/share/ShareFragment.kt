/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.share

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentShareBinding
import nl.rijksoverheid.en.onboarding.OnboardingViewModel
import nl.rijksoverheid.en.util.ext.setSlideTransition

/**
 * ShareFragment for main navGraph usage
 */
class MainShareFragment : ShareFragment()

/**
 * ShareFragment for onboarding navGraph usage
 */
class OnboardingShareFragment : ShareFragment() {
    private val onboardingViewModel: OnboardingViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSlideTransition()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            onboardingViewModel.finishOnboarding()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            onboardingViewModel.finishOnboarding()
        }

        binding.next.isVisible = true
        binding.nextButtonClickListener =
            View.OnClickListener { onboardingViewModel.finishOnboarding() }

        onboardingViewModel.onboardingComplete.observe(viewLifecycleOwner) {
            enterTransition = null
            exitTransition = null
            findNavController().popBackStack(R.id.nav_onboarding, true)
        }
    }
}

/**
 * Fragment with info regarding sharing the app
 */
abstract class ShareFragment : BaseFragment(R.layout.fragment_share) {

    protected lateinit var binding: FragmentShareBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentShareBinding.bind(view)

        binding.toolbar.apply {
            setNavigationIcon(R.drawable.ic_close)
            setNavigationContentDescription(R.string.cd_close)
        }

        binding.shareButtonClickListener = View.OnClickListener { share() }
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
