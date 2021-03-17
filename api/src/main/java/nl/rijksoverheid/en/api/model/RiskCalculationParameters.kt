/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RiskCalculationParameters(
    val reportTypeWeights: List<Double>,
    val infectiousnessWeights: List<Double>,
    val attenuationBucketThresholds: List<Int>,
    val attenuationBucketWeights: List<Double>,
    val daysSinceExposureThreshold: Int,
    val minimumWindowScore: Double,
    val minimumRiskScore: Double,
    val daysSinceOnsetToInfectiousness: List<DaySinceOnsetToInfectiousness>,
    val infectiousnessWhenDaysSinceOnsetMissing: Int,
    val reportTypeWhenMissing: Int
)

@JsonClass(generateAdapter = true)
data class DaySinceOnsetToInfectiousness(
    val daysSinceOnsetOfSymptoms: Int,
    val infectiousness: Int
)
