/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.enapi

import com.google.android.gms.nearby.exposurenotification.ExposureWindow

sealed class ExposureWindowsResult {

    /**
     * Exposure Windows returned from the API
     */
    data class Success(val exposureWindows: List<ExposureWindow>) : ExposureWindowsResult()

    /**
     * An unexpected API error occurred
     */
    data class UnknownError(val exception: Exception) : ExposureWindowsResult()
}
