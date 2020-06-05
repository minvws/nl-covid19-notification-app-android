/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.enapi

sealed class DisableNotificationsResult {
    /**
     * Exposure notifications api has stopped
     */
    object Disabled : DisableNotificationsResult()

    /**
     * An unexpected error occurred
     */
    data class UnknownError(val exception: Exception) : DisableNotificationsResult()
}
