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
            durationHours > 0 && durationMinutes > 0 -> {
                context.getString(
                    R.string.duration_format_hours_and_minutes,
                    formattedHours(durationHours, context),
                    formattedMinutes(durationMinutes, context)
                )
            }
            durationHours > 0 -> formattedHours(durationHours, context)
            durationMinutes > 0 -> formattedMinutes(durationMinutes, context)
            else -> ""
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
        val duration = Duration.between(LocalDateTime.now(), pausedUntil)
        val durationHours = duration.toHours()
        val durationMinutes = duration.toMinutes() - durationHours * 60
        Pair(durationHours, durationMinutes)
    } else Pair(0, 0)
}

private fun formattedHours(hours: Long, context: Context)
    = context.resources.getQuantityString(R.plurals.duration_hours_plurals, hours.toInt(), hours)

private fun formattedMinutes(minutes: Long, context: Context)
    = context.resources.getQuantityString(R.plurals.duration_minutes_plurals, minutes.toInt(), minutes)