/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.enapi

import java.lang.Exception

sealed class UpdateToDateResult {

    /**
     * The version of ExposureNotification API contains all features we require
     */
    object UpToDate: UpdateToDateResult()

    /**
     * ExposureNotification API requires an update
     */
    object RequiresAnUpdate: UpdateToDateResult()

    /**
     * Unable to check if ExposureNotification API is up to date
     */
    data class UnknownError(val exception: Exception): UpdateToDateResult()

}