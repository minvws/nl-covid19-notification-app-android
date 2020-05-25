/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.enapi

sealed class StopResult {
    /**
     * Exposure notifications api has stopped
     */
    object Stopped : StopResult()

    /**
     * An unexpected error occurred
     */
    data class UnknownError(val ex: Exception) : StopResult()
}
