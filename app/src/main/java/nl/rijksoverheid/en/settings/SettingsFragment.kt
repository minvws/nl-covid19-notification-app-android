/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.settings

import android.os.Bundle
import android.os.CountDownTimer
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
import nl.rijksoverheid.en.lifecyle.EventObserver
import nl.rijksoverheid.en.navigation.getBackStackEntryObserver
import nl.rijksoverheid.en.navigation.navigateCatchingErrors
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class SettingsFragment : BaseFragment(R.layout.fragment_settings) {

    private val viewModel: ExposureNotificationsViewModel by activityViewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private var pausedDurationTimer: CountDownTimer? = null

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
                if (settingsViewModel.skipPauseConfirmation) {
                    showPauseDurationBottomSheet()
                } else {
                    findNavController().navigateCatchingErrors(SettingsFragmentDirections.actionPauseConfirmation())
                }
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

        settingsViewModel.pausedState.observe(viewLifecycleOwner) {
            val now = LocalDateTime.now()
            if (it is Settings.PausedState.Paused && it.pausedUntil.isAfter(now)) {
                pausedDurationTimer?.cancel()
                pausedDurationTimer = object : CountDownTimer(now.until(it.pausedUntil, ChronoUnit.MILLIS), 5000) {
                    override fun onTick(p0: Long) {
                        binding.viewModel = settingsViewModel
                    }
                    override fun onFinish() {
                        binding.viewModel = settingsViewModel
                    }
                }.start()
            } else if (it !is Settings.PausedState.Paused && pausedDurationTimer != null) {
                pausedDurationTimer?.cancel()
                pausedDurationTimer = null
            }
        }
    }

    override fun onPause() {
        pausedDurationTimer?.cancel()
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
            SettingsFragmentDirections.actionSelectPauseDation()
        )
    }
}
