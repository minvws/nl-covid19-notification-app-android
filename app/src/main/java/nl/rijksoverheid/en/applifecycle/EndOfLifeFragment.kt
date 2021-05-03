/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.applifecycle

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.browser.customtabs.CustomTabsIntent
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentEndOfLifeBinding

class EndOfLifeFragment : BaseFragment(R.layout.fragment_end_of_life) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentEndOfLifeBinding.bind(view)
        binding.next.setOnClickListener {
            CustomTabsIntent.Builder().build().launchUrl(
                requireContext(),
                Uri.parse(getString(R.string.coronamelder_url, getString(R.string.app_language)))
            )
        }
    }
}
