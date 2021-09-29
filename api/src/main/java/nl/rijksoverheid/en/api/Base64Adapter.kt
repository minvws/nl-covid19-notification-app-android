/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api

import android.util.Base64
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

/**
 * Base64 type converter for converting to [ByteArray] to and from a base64 encoded [String]
 */
class Base64Adapter {
    @FromJson
    fun fromBase64(value: String): ByteArray = Base64.decode(value, 0)

    @ToJson
    fun toBase64(value: ByteArray) = Base64.encodeToString(value, Base64.NO_WRAP)
}
