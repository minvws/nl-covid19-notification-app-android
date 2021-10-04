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
import nl.rijksoverheid.en.databinding.FragmentPauseConfirmationBinding
import nl.rijksoverheid.en.navigation.getBackStackEntryObserver
import nl.rijksoverheid.en.navigation.navigateCatchingErrors
import java.time.LocalDateTime

/**
 * Confirmation fragment for pausing the app.
 */
class PauseConfirmationFragment : BaseFragment(R.layout.fragment_pause_confirmation) {

    private val viewModel: ExposureNotificationsViewModel by activityViewModels()
    private val pauseConfirmationViewModel: PauseConfirmationViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentPauseConfirmationBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = pauseConfirmationViewModel

        binding.acceptButtonClickListener = View.OnClickListener {
            showPauseDurationBottomSheet()
        }

        binding.declineButtonClickListener = View.OnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun showPauseDurationBottomSheet() {
        val navBackStackEntry = findNavController().getBackStackEntry(R.id.nav_pause_confirmation)
        val observer = navBackStackEntry.getBackStackEntryObserver<LocalDateTime>(
            KEY_PAUSE_DURATION_RESULT
        ) {
            pauseConfirmationViewModel.setExposureNotificationsPaused(it)
            viewModel.disableExposureNotifications()
            findNavController().popBackStack()
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
            PauseConfirmationFragmentDirections.actionSelectPauseDuration()
        )
    }
}
