/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RiskCalculationParameters(
    @Json(name = "minimumRiskScore") val minimumRiskScore: Int,
    @Json(name = "attenuationScores") val attenuationScores: List<Int>,
    @Json(name = "daysSinceLastExposureScores") val daysSinceLastExposureScores: List<Int>,
    @Json(name = "durationScores") val durationScores: List<Int>,
    @Json(name = "transmissionRiskScores") val transmissionRiskScores: List<Int>,
    @Json(name = "durationAtAttenuationThresholds") val durationAtAttenuationThresholds: List<Int>
)
