/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.config

import android.content.Context
import androidx.core.content.edit
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.BuildConfig

private const val KEY_TEST_PHASE = "test_phase"

/**
 * Temporary measure while running in the test & research phase of the project. Used to display
 * test indicators to the user. Needs to be removed after national roll-out.
 */
fun Context.saveIsTestPhaseVersion(isTestPhaseVersion: Boolean) =
    getSharedPreferences("${BuildConfig.APPLICATION_ID}.testphase", 0).edit {
        putBoolean(KEY_TEST_PHASE, isTestPhaseVersion)
    }

/**
 * Temporary measure while running in the test & research phase of the project. Used to display
 * test indicators to the user. Needs to be removed after national roll-out.
 */
fun BaseFragment.isTestPhaseVersion() =
    requireContext().getSharedPreferences("${BuildConfig.APPLICATION_ID}.testphase", 0)
        .getBoolean(KEY_TEST_PHASE, false)
