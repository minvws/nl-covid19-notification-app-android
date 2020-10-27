/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import androidx.annotation.Keep
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Retry with increasing delay
 * @param times the number of retries
 * @param initialDelay the initial delay
 * @param maxDelay the maximum delay between retries
 * @param factor the factor to increase delay time each retry
 * @param block the block to execute
 * @throws RetryException if the block did not complete without an exception after exhausting retries
 */
suspend fun <T> retry(
    times: Int = 15,
    initialDelay: Long = 25,
    maxDelay: Long = 3000,
    factor: Double = 1.5,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (ex: Exception) {
            // ignore
        }
        Timber.d("Retry, delay for $currentDelay ms")
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    try {
        return block()
    } catch (ex: Exception) {
        throw RetryException("Did not complete after retry", ex)
    }
}

@Keep
private class RetryException(message: String, cause: Exception) : RuntimeException(message, cause)
