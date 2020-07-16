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
class PostKeysRequest(
    val keys: List<TemporaryExposureKey>,
    @Json(name = "bucketId") val bucketId: String,
    val padding: ByteArray = ByteArray(0)
)

@JsonClass(generateAdapter = true)
class TemporaryExposureKey(
    val keyData: ByteArray,
    val rollingStartNumber: Int,
    val rollingPeriod: Int
)
