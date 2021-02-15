/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api.typeconverter

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import nl.rijksoverheid.en.api.model.WindowCalculationType

object WindowCalculationTypeConverter {

    @FromJson
    fun toWindowCalculationType(value: Int?): WindowCalculationType? {
        return WindowCalculationType.getByValue(value ?: return null)
    }

    @ToJson
    fun fromWindowCalculationType(windowCalculationType: WindowCalculationType?): Int? {
        return windowCalculationType?.value
    }
}
