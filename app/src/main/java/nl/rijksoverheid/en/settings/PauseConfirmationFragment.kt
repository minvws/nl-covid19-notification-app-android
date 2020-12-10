/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
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
import nl.rijksoverheid.en.databinding.FragmentPauseConfirmationBinding

class PauseConfirmationFragment : BaseFragment(R.layout.fragment_pause_confirmation) {

    private val viewModel: ExposureNotificationsViewModel by activityViewModels()
    private val pauseConfirmationViewModel: PauseConfirmationViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentPauseConfirmationBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = pauseConfirmationViewModel

        binding.acceptButtonClickListener = View.OnClickListener {
            // TODO should be replaced pause duration bottomSheet
            pauseConfirmationViewModel.setExposureNotificationsPaused()
            viewModel.disableExposureNotifications()
            findNavController().popBackStack()
        }

        binding.declineButtonClickListener = View.OnClickListener {
            findNavController().popBackStack()
        }

    }
}