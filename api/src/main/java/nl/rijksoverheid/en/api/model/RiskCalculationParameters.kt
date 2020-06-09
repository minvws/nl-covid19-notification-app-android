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
    @Json(name = "MinimumRiskScore") val minimumRiskScore: Int,
    @Json(name = "AttenuationScores") val attenuationScores: List<Int>,
    @Json(name = "DaysSinceLastExposureScores") val daysSinceLastExposureScores: List<Int>,
    @Json(name = "DurationScores") val durationScores: List<Int>,
    @Json(name = "TransmissionRiskScores") val transmissionRiskScores: List<Int>,
    @Json(name = "DurationAtAttenuationThresholds") val durationAtAttenuationThresholds: List<Int>
)
