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
data class Manifest(
    @Json(name = "ExposureKeySets") val exposureKeysSetIds: List<String>,
    @Json(name = "ResourceBundle") val resourceBundleId: String,
    @Json(name = "RiskCalculationParameters") val riskCalculationParametersId: String,
    @Json(name = "AppConfig") val appConfigId: String
)
