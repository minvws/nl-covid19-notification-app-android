/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.api.model

import com.squareup.moshi.JsonClass
import java.time.OffsetDateTime

@JsonClass(generateAdapter = true)
data class AppMessage (
    val scheduledDateTime: OffsetDateTime,
    val title: String,
    val body: String,
    val targetScreen: String
) {
    /**
     * All implemented target screen by value
     */
    enum class TargetScreenOption(val value: String) {
        SHARE("share"),
        MAIN("main")
    }
}

