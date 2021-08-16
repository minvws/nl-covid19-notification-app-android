/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util.ext

import android.content.SharedPreferences
import java.time.LocalDate

fun SharedPreferences.getLongAsLocalDate(sharedPreferenceKey: String): LocalDate? {
    val timestamp = getLong(sharedPreferenceKey, 0L)
    return if (timestamp > 0) {
        LocalDate.ofEpochDay(timestamp)
    } else {
        null
    }
}
