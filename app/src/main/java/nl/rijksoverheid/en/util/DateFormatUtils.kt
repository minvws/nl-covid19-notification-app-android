/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import android.content.Context
import android.text.format.DateFormat
import nl.rijksoverheid.en.R
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Creates formatted string that represents the number of days ago of this date.
 */
fun LocalDate.formatDaysSince(context: Context, clock: Clock = Clock.systemDefaultZone()): String {
    val daysSince = ChronoUnit.DAYS.between(this, LocalDate.now(clock)).toInt()
    return if (daysSince == 0) context.resources.getString(R.string.today) else
        context.resources.getQuantityString(R.plurals.days_ago, daysSince, daysSince)
}

/**
 * Formats the exposure date using the correct locale.
 */
fun LocalDate.formatExposureDate(context: Context): String {
    val locale = Locale(context.getString(R.string.app_language))
    val format = DateFormat.getBestDateTimePattern(locale, context.getString(R.string.exposure_date_format))
    return DateTimeFormatter.ofPattern(format, locale)
        .withDecimalStyle(DecimalStyle.of(locale))
        .format(this)
}

/**
 * Formats the exposure date using the correct locale.
 */
fun LocalDate.formatExposureDateShort(context: Context): String {
    val locale = Locale(context.getString(R.string.app_language))
    val format = DateFormat.getBestDateTimePattern(locale, context.getString(R.string.exposure_date_short_format))
    return DateTimeFormatter.ofPattern(format, locale)
        .withDecimalStyle(DecimalStyle.of(locale))
        .format(this)
}

/**
 * Formats the dateTime using the correct locale.
 */
fun LocalDateTime.formatDateTime(context: Context): String {
    val locale = Locale(context.getString(R.string.app_language))
    val format = DateFormat.getBestDateTimePattern(locale, context.getString(R.string.date_time_format))
    return DateTimeFormatter.ofPattern(format, locale)
        .withDecimalStyle(DecimalStyle.of(locale))
        .format(this)
}

/**
 * Formats the dashboard date using the correct locale.
 */
fun LocalDate.formatDashboardDateShort(context: Context, clock: Clock = Clock.systemDefaultZone()): String {
    return when (ChronoUnit.DAYS.between(this, LocalDate.now(clock)).toInt()) {
        0 -> context.resources.getString(R.string.today)
        1 -> context.resources.getString(R.string.yesterday)
        2 -> context.resources.getString(R.string.day_before_yesterday)
        else -> formatDateShort(context)
    }
}

fun LocalDate.formatDateShort(context: Context): String {
    val locale = Locale(context.getString(R.string.app_language))
    val format = DateFormat.getBestDateTimePattern(locale, context.getString(R.string.date_short_format))
    return DateTimeFormatter.ofPattern(format, locale)
        .withDecimalStyle(DecimalStyle.of(locale))
        .format(this)
}

fun LocalDateTime.formatDate(context: Context): String {
    val locale = Locale(context.getString(R.string.app_language))
    val format = DateFormat.getBestDateTimePattern(locale, context.getString(R.string.date_format))
    return DateTimeFormatter.ofPattern(format, locale)
        .withDecimalStyle(DecimalStyle.of(locale))
        .format(this)
}
