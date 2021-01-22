/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import android.content.Context
import androidx.core.text.HtmlCompat
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.settings.Settings
import java.time.Duration
import java.time.LocalDateTime

fun Settings.PausedState.Paused.formatDuration(context: Context): CharSequence {
    val now = LocalDateTime.now()
    return if (pausedUntil.isAfter(now)) {

        val (durationHours, durationMinutes) = durationHoursAndMinutes()
        val formattedDuration = when {
            durationHours > 1L && durationMinutes > 1L -> context.getString(
                R.string.duration_format_hours_and_minutes,
                durationHours, durationMinutes
            )
            durationHours == 1L && durationMinutes > 1L -> context.getString(
                R.string.duration_format_hour_and_minutes,
                durationHours, durationMinutes
            )
            durationHours > 1L && durationMinutes == 1L -> context.getString(
                R.string.duration_format_hours_and_minute,
                durationHours, durationMinutes
            )
            durationHours == 1L && durationMinutes == 1L -> context.getString(
                R.string.duration_format_hour_and_minute,
                durationHours, durationMinutes
            )
            durationHours > 1L -> context.getString(
                R.string.duration_format_hours,
                durationHours
            )
            durationHours == 1L -> context.getString(
                R.string.duration_format_hour,
                durationHours
            )
            durationMinutes > 1L -> context.getString(
                R.string.duration_format_minutes,
                durationMinutes
            )
            durationMinutes == 1L -> context.getString(
                R.string.duration_format_minute,
                durationMinutes
            )
            else -> return context.getString(R.string.paused_en_api_duration_reached)
        }

        HtmlCompat.fromHtml(
            context.getString(
                R.string.paused_en_api_description,
                formattedDuration
            ),
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )
    } else {
        context.getString(R.string.paused_en_api_duration_reached)
    }
}

fun Settings.PausedState.Paused.durationHoursAndMinutes(): Pair<Long, Long> {
    val now = LocalDateTime.now()
    return if (pausedUntil.isAfter(now)) {
        // Get duration rounded up to minutes
        val duration = Duration.between(now, pausedUntil).let {
            it.plusMillis(60000 - (it.toMillis() % 60000))
        }
        val durationHours = duration.toHours()
        val durationMinutes = duration.toMinutes() - durationHours * 60
        Pair(durationHours, durationMinutes)
    } else Pair(0, 0)
}
