/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import nl.rijksoverheid.en.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class DateFormatUtilsTest {
    @Test
    fun testFormatDaysSinceSpanningMonth() {
        val clock = Clock.fixed(Instant.parse("2020-11-27T10:15:30.00Z"), ZoneId.of("UTC"))
        val exposure = LocalDate.parse("2020-10-21")
        val context = ApplicationProvider.getApplicationContext<Application>()

        assertEquals(
            context.resources.getQuantityString(R.plurals.days_ago, 37, 37),
            exposure.formatDaysSince(context, clock)
        )
    }

    @Test
    fun testFormatDaysSinceSameMonth() {
        val clock = Clock.fixed(Instant.parse("2020-11-27T10:15:30.00Z"), ZoneId.of("UTC"))
        val exposure = LocalDate.parse("2020-11-21")
        val context = ApplicationProvider.getApplicationContext<Application>()

        assertEquals(
            context.resources.getQuantityString(R.plurals.days_ago, 6, 6),
            exposure.formatDaysSince(context, clock)
        )
    }

    @Test
    fun testFormatDaysSameDay() {
        val clock = Clock.fixed(Instant.parse("2020-11-21T10:15:30.00Z"), ZoneId.of("UTC"))
        val exposure = LocalDate.parse("2020-11-21")
        val context = ApplicationProvider.getApplicationContext<Application>()

        assertEquals(
            context.resources.getString(R.string.today),
            exposure.formatDaysSince(context, clock)
        )
    }
}
