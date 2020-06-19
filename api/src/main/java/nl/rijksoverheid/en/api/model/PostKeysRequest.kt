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
data class PostKeysRequest(
    val keys: List<TemporaryExposureKey>,
    @Json(name = "bucketID") val bucketId: String,
    val padding: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PostKeysRequest

        if (keys != other.keys) return false
        if (padding != null) {
            if (other.padding == null) return false
            if (!padding.contentEquals(other.padding)) return false
        } else if (other.padding != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = keys.hashCode()
        result = 31 * result + (padding?.contentHashCode() ?: 0)
        return result
    }
}

@JsonClass(generateAdapter = true)
class TemporaryExposureKey(
    val keyData: ByteArray,
    val rollingStartNumber: Int,
    val rollingPeriod: Int = DEFAULT_ROLLING_PERIOD
) {
    companion object {
        const val DEFAULT_ROLLING_PERIOD = 144
    }
}
