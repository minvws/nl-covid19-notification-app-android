/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.job

interface BackgroundWorkScheduler {
    /**
     * Schedule background work. Invoked when the app is activated the first time.
     * @param intervalMinutes interval for checking the manifest as part of background work
     */
    fun schedule(intervalMinutes: Int)

    /**
     * Cancel all scheduled background work
     */
    fun cancel()
}
