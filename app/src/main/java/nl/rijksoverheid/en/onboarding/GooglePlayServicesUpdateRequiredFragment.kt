/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.onboarding

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.GoogleApiAvailability
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentGooglePlayServicesUpgradeRequiredBinding
import nl.rijksoverheid.en.util.IntentHelper
import nl.rijksoverheid.en.util.ext.setSlideTransition

class GooglePlayServicesUpdateRequiredFragment :
    BaseFragment(R.layout.fragment_google_play_services_upgrade_required) {

    val viewModel: OnboardingViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSlideTransition()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentGooglePlayServicesUpgradeRequiredBinding.bind(view)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            requireActivity().finish()
        }

        binding.next.setOnClickListener {
            IntentHelper.openPlayStore(requireActivity(), GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE)
        }

        viewModel.isExposureNotificationApiUpToDate.observe(viewLifecycleOwner) { upToDate ->
            if (upToDate) {
                findNavController().popBackStack()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshExposureNotificationApiUpToDate()
    }
}
