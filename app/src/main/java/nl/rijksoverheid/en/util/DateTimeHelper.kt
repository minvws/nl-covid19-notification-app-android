/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

object DateTimeHelper {

    fun convertToLocalDate(timestamp: Long): LocalDate {
        return Instant.ofEpochMilli(timestamp)
            .atOffset(ZoneOffset.UTC)
            .toLocalDate()
    }
}