/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FeatureFlag(val id: String, val featureEnabled: Boolean)

/**
 * All implemented feature flags by id
 */
enum class FeatureFlagOption(val id: String) {
    INDEPENDENT_KEY_SHARING("independentKeySharing")
}
