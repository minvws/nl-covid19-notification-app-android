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
data class AppConfig(
    @Json(name = "androidMinimumKillVersion") val requiredAppVersionCode: Int,
    @Json(name = "manifestFrequency") val updatePeriodMinutes: Int,
    @Json(name = "decoyProbability") val decoyProbability: Int
)
