/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.util

import android.content.Context
import nl.rijksoverheid.en.R
import java.time.Clock
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Creates formatted string that represents the number of days ago of this date.
 */
fun LocalDate.formatDaysSince(context: Context, clock: Clock = Clock.systemDefaultZone()): String {
    val daysSince = Period.between(this, LocalDate.now(clock)).days
    return if (daysSince == 0) context.resources.getString(R.string.today) else
        context.resources.getQuantityString(R.plurals.days, daysSince, daysSince)
}

/**
 * Formats the exposure date using the correct locale.
 */
fun LocalDate.formatExposureDate(context: Context): String = DateTimeFormatter.ofPattern(
    context.getString(R.string.exposure_date_format),
    Locale(context.getString(R.string.app_language))
).format(this)