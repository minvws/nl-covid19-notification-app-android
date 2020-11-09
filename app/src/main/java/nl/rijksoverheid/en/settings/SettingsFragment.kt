/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commitNow
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.ExposureNotificationsViewModel
import nl.rijksoverheid.en.R

class SettingsFragment : BaseFragment(R.layout.fragment_settings) {

    private val viewModel: ExposureNotificationsViewModel by activityViewModels()

    private val sharedPreferenceListener: SharedPreferences.OnSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key -> onPreferenceChanged(key) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fragment = childFragmentManager.findFragmentById(R.id.settings)
        if (fragment == null) {
            childFragmentManager.commitNow {
                add(R.id.settings, SettingsPreferenceFragment())
            }
        }
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .registerOnSharedPreferenceChangeListener(sharedPreferenceListener)
    }

    private fun onPreferenceChanged(key: String) {
        when (key) {
            Settings.KEY_WIFI_ONLY -> viewModel.rescheduleBackgroundJobs()
        }
    }

    override fun onDestroyView() {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .unregisterOnSharedPreferenceChangeListener(sharedPreferenceListener)
        super.onDestroyView()
    }

    class SettingsPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings, rootKey)
        }
    }
}
