/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.status

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.coroutineScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.collect
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.ExposureNotificationsViewModel
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentLocationServicesRequiredBinding
import nl.rijksoverheid.en.navigation.navigateCatchingErrors
import nl.rijksoverheid.en.util.openLocationSettings

class LocationServicesRequiredFragment :
    BaseFragment(R.layout.fragment_location_services_required) {

    private val viewModel: ExposureNotificationsViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentLocationServicesRequiredBinding.bind(view)

        binding.enableLocationServicesClickListener = View.OnClickListener {
            openLocationSettings()
        }

        binding.explanationClickListener = View.OnClickListener {
            findNavController().navigateCatchingErrors(LocationServicesRequiredFragmentDirections.actionShowFaq())
        }

        viewLifecycleOwner.lifecycle.coroutineScope.launchWhenStarted {
            viewModel.observeLocationPreconditionSatisfied(requireContext()).collect {
                findNavController().popBackStack()
            }
        }
    }
}