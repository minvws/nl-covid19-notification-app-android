/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.enapi

sealed class StatusResult {
    /**
     * Exposure notifications API has been enabled
     */
    object Enabled : StatusResult()

    /**
     * Exposure notifications API has been disabled
     */
    object Disabled : StatusResult()

    /**
     * Exposure notifications API is not available
     */
    data class Unavailable(val statusCode: Int) : StatusResult()

    /**
     * An unknown error has occurred
     */
    data class UnknownError(val exception: Exception) : StatusResult()
}
