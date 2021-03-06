/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import nl.rijksoverheid.en.api.createMoshi
import okio.buffer
import okio.source
import java.io.InputStream

@JsonClass(generateAdapter = true)
class ResourceBundle(
    @Json(name = "resources") val resources: Map<String, Map<String, String>>,
    @Json(name = "guidance") val guidance: Guidance
) {
    @JsonClass(generateAdapter = true)
    class Guidance(
        val layoutByRelativeExposureDay: List<RelativeExposureDayLayout> = emptyList(),
        val layout: List<Element>
    ) {
        sealed class Element {
            @JsonClass(generateAdapter = true)
            class Paragraph(val title: String, val body: String) : Element()
            object Unknown : Element()
        }

        @JsonClass(generateAdapter = true)
        class RelativeExposureDayLayout(
            val exposureDaysLowerBoundary: Int? = null,
            val exposureDaysUpperBoundary: Int? = null,
            val layout: List<Element>
        )
    }

    companion object {
        fun load(inputStream: InputStream): ResourceBundle? {
            val moshi = createMoshi()
            return moshi.adapter(ResourceBundle::class.java).fromJson(inputStream.source().buffer())
        }
    }
}
