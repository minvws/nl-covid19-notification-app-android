/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.enapi

import android.app.PendingIntent

sealed class StartResult {
    /**
     * Exposure notifications api is started
     */
    object Started : StartResult()

    /**
     * The user needs to give consent before the api can be started.
     */
    data class ResolutionRequired(val resolution: PendingIntent) : StartResult()

    /**
     * An unexpected error occurred
     */
    data class UnknownError(val ex: Exception) : StartResult()
}
