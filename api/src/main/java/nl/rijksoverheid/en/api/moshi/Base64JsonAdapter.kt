/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api.moshi

import android.util.Base64
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

internal class Base64JsonAdapter {
    @FromJson
    fun fromJson(string: String?): ByteArray {
        return Base64.decode(string, Base64.NO_WRAP)
    }

    @ToJson
    fun toJson(bytes: ByteArray?): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
