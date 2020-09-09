/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.status

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.coroutineScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.collect
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.ExposureNotificationsViewModel
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentLocationServicesRequiredBinding
import timber.log.Timber

class LocationServicesRequiredFragment :
    BaseFragment(R.layout.fragment_location_services_required) {

    private val viewModel: ExposureNotificationsViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentLocationServicesRequiredBinding.bind(view)

        binding.enableLocationServicesClickListener = View.OnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            } catch (ex: ActivityNotFoundException) {
                Timber.e(ex, "Error opening location services settings")
                Toast.makeText(
                    requireContext(),
                    R.string.location_services_required_enable_error,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        viewLifecycleOwner.lifecycle.coroutineScope.launchWhenStarted {
            viewModel.observeLocationPreconditionSatisfied(requireContext()).collect {
                findNavController().popBackStack()
            }
        }
    }
}