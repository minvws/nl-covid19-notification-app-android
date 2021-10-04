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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.fragment.findNavController
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.ExposureNotificationsViewModel
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentSettingsBinding
import nl.rijksoverheid.en.lifecyle.observeEvent
import nl.rijksoverheid.en.navigation.getBackStackEntryObserver
import nl.rijksoverheid.en.navigation.navigateCatchingErrors
import nl.rijksoverheid.en.util.LocaleHelper
import nl.rijksoverheid.en.util.SimpleCountdownTimer
import java.time.LocalDateTime

/**
 * Fragment for configuring in app settings like pausing, wifi only and language.
 */
class SettingsFragment : BaseFragment(R.layout.fragment_settings) {

    private val viewModel: ExposureNotificationsViewModel by activityViewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val localeHelper = LocaleHelper.getInstance()

    private var pausedDurationTimer: SimpleCountdownTimer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentSettingsBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = settingsViewModel

        binding.isSystemLanguageDutch = localeHelper.isSystemLanguageDutch
        binding.useAppInDutchSwitch.isChecked = localeHelper.isAppSetToDutch
        binding.useAppInDutchSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (localeHelper.useAppInDutch(isChecked, requireContext())) {
                // Ensure current fragment is updated after changing language
                parentFragmentManager.beginTransaction().detach(this).commitAllowingStateLoss()
                parentFragmentManager.beginTransaction().attach(this).commitAllowingStateLoss()
            }
        }

        settingsViewModel.wifiOnlyChanged.observeEvent(viewLifecycleOwner) {
            viewModel.rescheduleBackgroundJobs()
        }

        settingsViewModel.pauseRequested.observeEvent(viewLifecycleOwner) {
            if (settingsViewModel.skipPauseConfirmation) {
                showPauseDurationBottomSheet()
            } else {
                findNavController().navigateCatchingErrors(SettingsFragmentDirections.actionPauseConfirmation())
            }
        }

        settingsViewModel.enableExposureNotificationsRequested.observeEvent(viewLifecycleOwner) {
            if (viewModel.locationPreconditionSatisfied) {
                viewModel.requestEnableNotificationsForcingConsent()
            } else {
                findNavController().navigateCatchingErrors(SettingsFragmentDirections.actionEnableLocationServices())
            }
        }

        settingsViewModel.enableExposureNotificationsRequested.observeEvent(viewLifecycleOwner) {
            if (viewModel.locationPreconditionSatisfied) {
                viewModel.requestEnableNotificationsForcingConsent()
            } else {
                findNavController().navigateCatchingErrors(SettingsFragmentDirections.actionEnableLocationServices())
            }
        }

        settingsViewModel.pausedState.observe(viewLifecycleOwner) {
            val now = LocalDateTime.now()
            if (it is Settings.PausedState.Paused && it.pausedUntil.isAfter(now)) {
                pausedDurationTimer?.cancel()
                pausedDurationTimer = SimpleCountdownTimer(it.pausedUntil) {
                    binding.viewModel = settingsViewModel
                }
                pausedDurationTimer?.startTimer()
            } else if (it !is Settings.PausedState.Paused && pausedDurationTimer != null) {
                pausedDurationTimer?.cancelTimer()
                pausedDurationTimer = null
            }
        }
    }

    override fun onResume() {
        super.onResume()
        pausedDurationTimer?.startTimer()
    }

    override fun onPause() {
        pausedDurationTimer?.cancelTimer()
        pausedDurationTimer = null
        super.onPause()
    }

    private fun showPauseDurationBottomSheet() {
        val navBackStackEntry = findNavController().getBackStackEntry(R.id.nav_settings)
        val observer = navBackStackEntry.getBackStackEntryObserver<LocalDateTime>(
            KEY_PAUSE_DURATION_RESULT
        ) {
            settingsViewModel.setExposureNotificationsPaused(it)
            viewModel.disableExposureNotifications()
        }
        navBackStackEntry.lifecycle.addObserver(observer)
        viewLifecycleOwner.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) {
                    navBackStackEntry.lifecycle.removeObserver(observer)
                }
            }
        )
        findNavController().navigateCatchingErrors(
            SettingsFragmentDirections.actionSelectPauseDuration()
        )
    }
}
