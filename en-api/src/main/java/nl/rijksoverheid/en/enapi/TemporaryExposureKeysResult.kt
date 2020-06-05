/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.enapi

import android.app.PendingIntent
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey

sealed class TemporaryExposureKeysResult {
    /**
     * Keys returned from the API
     */
    data class Success(val keys: List<TemporaryExposureKey>) : TemporaryExposureKeysResult()

    /**
     * Consent is required before exporting the keys.
     */
    data class RequireConsent(val resolution: PendingIntent) : TemporaryExposureKeysResult()

    /**
     * An unexpected API error occurred
     */
    data class UnknownError(val exception: Exception) : TemporaryExposureKeysResult()
}
