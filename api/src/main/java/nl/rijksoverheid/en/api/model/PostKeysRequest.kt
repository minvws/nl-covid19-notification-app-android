/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostKeysRequest(val keys: List<TemporaryExposureKey>, val padding: String = "")

@JsonClass(generateAdapter = true)
data class TemporaryExposureKey(
    val keyData: ByteArray,
    val rollingStartNumber: Int,
    val rollingPeriod: Int = 144,
    val regionsOfInterest: List<String> = listOf("NL")
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TemporaryExposureKey

        if (!keyData.contentEquals(other.keyData)) return false
        if (rollingStartNumber != other.rollingStartNumber) return false
        if (rollingPeriod != other.rollingPeriod) return false
        if (regionsOfInterest != other.regionsOfInterest) return false

        return true
    }

    override fun hashCode(): Int {
        var result = keyData.contentHashCode()
        result = 31 * result + rollingStartNumber
        result = 31 * result + rollingPeriod
        result = 31 * result + regionsOfInterest.hashCode()
        return result
    }
}
