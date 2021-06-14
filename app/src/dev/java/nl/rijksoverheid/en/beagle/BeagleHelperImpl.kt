/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.beagle

import android.app.Application

object BeagleHelperImpl : BeagleHelper {

    override val useDefaultGuidance: Boolean = false
    override val useDebugFeatureFlags: Boolean = false
    override val testExposureDaysAgo: Int = 5
    override val debugFeatureFlags: List<FeatureFlag> = emptyList()

    override fun initialize(application: Application) {
        // no-op
    }
}
