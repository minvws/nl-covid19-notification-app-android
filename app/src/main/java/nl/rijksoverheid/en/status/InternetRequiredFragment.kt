/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentInternetRequiredBinding
import nl.rijksoverheid.en.navigation.navigateCatchingErrors

class InternetRequiredFragment :
    BaseFragment(R.layout.fragment_internet_required) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentInternetRequiredBinding.bind(view)

        binding.toolbar.apply {
            setNavigationIcon(R.drawable.ic_close)
        }

        binding.openSettingsClickListener = View.OnClickListener {
            findNavController().navigateCatchingErrors(InternetRequiredFragmentDirections.actionShowSettings())
        }
    }
}
