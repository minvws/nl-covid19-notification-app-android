/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.navigation.NavController
import androidx.navigation.NavDestination

/**
 * Add/clear FLAG_SECURE window flag to treat the content of the window as secure,
 * preventing it from appearing in screenshots or from being viewed on non-secure displays.
 *
 * @param window: target window for applying the flag on
 * @param secureDestinations: single or multiple navGraph destinations which should contain the secure flag
 */
class SecureScreenNavigationListener(
    private val window: Window,
    private vararg val secureDestinations: Int
) : NavController.OnDestinationChangedListener {
    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        if (secureDestinations.contains(destination.id)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
