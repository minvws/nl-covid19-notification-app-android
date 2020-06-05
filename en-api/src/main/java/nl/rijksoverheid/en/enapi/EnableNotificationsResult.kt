/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.enapi

import android.app.PendingIntent

sealed class EnableNotificationsResult {
    /**
     * Exposure notifications api is started
     */
    object Enabled : EnableNotificationsResult()

    /**
     * The user needs to give consent before the api can be started.
     */
    data class ResolutionRequired(val resolution: PendingIntent) : EnableNotificationsResult()

    /**
     * An unexpected error occurred
     */
    data class UnknownError(val exception: Exception) : EnableNotificationsResult()
}
