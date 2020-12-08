/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.ExposureNotificationsViewModel
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentSettingsBinding
import nl.rijksoverheid.en.lifecyle.EventObserver
import nl.rijksoverheid.en.navigation.navigateCatchingErrors

class SettingsFragment : BaseFragment(R.layout.fragment_settings) {

    private val viewModel: ExposureNotificationsViewModel by activityViewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentSettingsBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = settingsViewModel

        settingsViewModel.wifiOnlyChanged.observe(
            viewLifecycleOwner,
            EventObserver {
                viewModel.rescheduleBackgroundJobs()
            }
        )

        settingsViewModel.pauseRequested.observe(
            viewLifecycleOwner,
            EventObserver {
                viewModel.disableExposureNotifications()
            }
        )

        settingsViewModel.enableExposureNotificationsRequested.observe(
            viewLifecycleOwner,
            EventObserver {
                if (viewModel.locationPreconditionSatisfied) {
                    viewModel.requestEnableNotificationsForcingConsent()
                } else {
                    findNavController().navigateCatchingErrors(SettingsFragmentDirections.actionEnableLocationServices())
                }
            }
        )
    }
}
