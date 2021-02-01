/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.atomic.AtomicInteger

@RunWith(JUnit4::class)
class RetryUtilTest {
    @Test
    fun `retry without failures executes the given block once`() = runBlocking {
        val attempts = AtomicInteger(0)
        retry {
            attempts.incrementAndGet()
        }
        assertEquals(1, attempts.get())
    }

    @Test
    fun `retry with failures executes the given block and returns when successful`() = runBlocking {
        val attempts = AtomicInteger(0)
        retry {
            val attempt = attempts.incrementAndGet()
            if (attempt < 3) {
                throw RuntimeException("Failure!")
            }
        }
        assertEquals(3, attempts.get())
    }

    @Test
    fun `retry with failures executes the given block until max attempts and then throws`() =
        runBlocking {
            val exception = RuntimeException("Failure!")
            val attempts = AtomicInteger(0)
            try {
                retry(times = 2) {
                    val attempt = attempts.incrementAndGet()
                    if (attempt < 3) {
                        throw exception
                    }
                }
                fail("Exception expected")
            } catch (ex: Exception) {
                assertEquals(exception, ex.cause)
            }
        }
}
