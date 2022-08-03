/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
@file:Suppress("ktlint:filename")

package nl.rijksoverheid.en.util.ext

import android.content.Context
import nl.rijksoverheid.en.R
import java.time.Duration
import java.time.LocalDateTime

fun LocalDateTime.formatPauseDuration(context: Context): String {
    val now = LocalDateTime.now()
    return if (isAfter(now)) {
        val (durationHours, durationMinutes) = durationHoursAndMinutes()
        val formattedDuration = when {
            durationHours > 0 && durationMinutes > 0 -> formattedHoursAndMinutes(durationHours, durationMinutes, context)
            durationHours > 0 -> formattedHours(durationHours, context)
            durationMinutes > 0 -> formattedMinutes(durationMinutes, context)
            else -> return context.getString(R.string.paused_en_api_duration_reached)
        }

        context.getString(
            R.string.paused_en_api_description,
            formattedDuration
        )
    } else {
        context.getString(R.string.paused_en_api_duration_reached)
    }
}

private fun LocalDateTime.durationHoursAndMinutes(): Pair<Long, Long> {
    val now = LocalDateTime.now()
    return if (isAfter(now)) {
        // Get duration rounded up to minutes
        val duration = Duration.between(now, this).let {
            it.plusMillis(60000 - (it.toMillis() % 60000))
        }
        val durationHours = duration.toHours()
        val durationMinutes = duration.toMinutes() - durationHours * 60
        Pair(durationHours, durationMinutes)
    } else Pair(0, 0)
}

private fun formattedHoursAndMinutes(hours: Long, minutes: Long, context: Context) =
    context.getString(
        R.string.duration_format_hours_and_minutes,
        formattedHours(hours, context),
        formattedMinutes(minutes, context)
    )

private fun formattedHours(hours: Long, context: Context) =
    context.resources.getQuantityString(R.plurals.duration_hours_plurals, hours.toInt(), hours)

private fun formattedMinutes(minutes: Long, context: Context) =
    context.resources.getQuantityString(R.plurals.duration_minutes_plurals, minutes.toInt(), minutes)
