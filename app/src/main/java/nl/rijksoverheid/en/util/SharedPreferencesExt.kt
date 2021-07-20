/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import android.content.SharedPreferences
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

fun SharedPreferences.getLongAsLocalDate(sharedPreferenceKey: String): LocalDate? {
    val timestamp = getLong(sharedPreferenceKey, 0L)
    return if (timestamp > 0) {
        LocalDate.ofEpochDay(timestamp)
    } else {
        null
    }
}

fun SharedPreferences.getLongAsLocalDateTime(sharedPreferenceKey: String): LocalDateTime? {
    val timestamp = getLong(sharedPreferenceKey, 0L)
    return if (timestamp > 0) {
        LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    } else {
        null
    }
}
