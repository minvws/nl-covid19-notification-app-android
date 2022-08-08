/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
@file:Suppress("ktlint:filename")

package nl.rijksoverheid.en.util

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import timber.log.Timber

/**
 * Opens the system location services settings, or shows a toast when this fails.
 */
fun BaseFragment.openLocationSettings() {
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
