/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util.ext

import nl.rijksoverheid.en.api.model.AppMessage
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.OffsetDateTime

@RunWith(RobolectricTestRunner::class)
class AppMessageExtTest {

    @Test
    fun `shouldScheduleBasedOnProbability returns true with probability of 1`() {
        val appMessage = AppMessage(
            OffsetDateTime.now(),
            "title",
            "body",
            "main",
            1f
        )

        Assert.assertTrue(appMessage.shouldScheduleBasedOnProbability())
    }

    @Test
    fun `shouldScheduleBasedOnProbability returns false with probability of 0`() {
        val appMessage = AppMessage(
            OffsetDateTime.now(),
            "title",
            "body",
            "main",
            0f
        )

        Assert.assertFalse(appMessage.shouldScheduleBasedOnProbability())
    }
}
