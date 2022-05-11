/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.navigation

import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigator
import timber.log.Timber
import java.lang.IllegalStateException

fun NavController.navigateCatchingErrors(directions: NavDirections) {
    try {
        navigate(directions)
    } catch (ex: IllegalArgumentException) {
        Timber.w(ex, "Error while navigating")
    }
}

fun NavController.navigateCatchingErrors(directions: NavDirections, extras: Navigator.Extras) {
    try {
        navigate(directions, extras)
    } catch (ex: IllegalArgumentException) {
        Timber.w(ex, "Error while navigating")
    }
}

fun NavController.isInitialised(): Boolean {
    return try {
        graph.startDestinationRoute != null
    } catch (ex: IllegalStateException) {
        false
    }
}
