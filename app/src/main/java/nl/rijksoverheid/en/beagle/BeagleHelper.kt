/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.beagle

import android.app.Application
import nl.rijksoverheid.en.api.model.FeatureFlag

/**
 * BeagleHelper should be implemented in build flavor specific files that should include the debug drawer.
 */
interface BeagleHelper {

    val useDefaultGuidance: Boolean
    val useDebugFeatureFlags: () -> Boolean
    val testExposureDaysAgo: Int
    val getDebugFeatureFlags: () -> List<FeatureFlag>

    fun initialize(application: Application)
}
