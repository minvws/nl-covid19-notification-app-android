/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.beagle

import android.app.Application
import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.api.model.FeatureFlag

interface DebugDrawer {

    val useDefaultGuidance: Boolean
    val useDebugFeatureFlags: () -> Boolean
    val testExposureDaysAgo: Int
    val getDebugFeatureFlags: () -> List<FeatureFlag>

    fun initialize(application: Application)
}

private class NoOpDebugDrawer : DebugDrawer {
    override val useDefaultGuidance: Boolean = false
    override val useDebugFeatureFlags = { false }
    override val testExposureDaysAgo = 5
    override val getDebugFeatureFlags = { emptyList<FeatureFlag>() }

    override fun initialize(application: Application) {
    }
}

val debugDrawer by lazy {
    if (BuildConfig.FEATURE_DEBUG_DRAWER) {
        DebugDrawerImpl()
    } else {
        NoOpDebugDrawer()
    }
}
