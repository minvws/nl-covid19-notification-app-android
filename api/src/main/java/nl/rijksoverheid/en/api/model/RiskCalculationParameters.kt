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
    @Json(name = "reportTypeWeights") val reportTypeWeights: List<Double>,
    @Json(name = "infectiousnessWeights") val infectiousnessWeights: List<Double>,
    @Json(name = "attenuationBucketThresholdDb") val attenuationBucketThresholdDb: List<Int>,
    @Json(name = "attenuationBucketWeights") val attenuationBucketWeights: List<Double>,
    @Json(name = "daysSinceExposureThreshold") val daysSinceExposureThreshold: Int,
    @Json(name = "minimumWindowScore") val minimumWindowScore: Double,
    @Json(name = "minimumRiskScore") val minimumRiskScore: Double,
    @Json(name = "daysSinceOnsetToInfectiousness") val daysSinceOnsetToInfectiousness: List<Int>
)