/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest

import android.app.PendingIntent
import kotlinx.coroutines.delay
import nl.rijksoverheid.en.enapi.TemporaryExposureKeysResult
import nl.rijksoverheid.en.enapi.nearby.ExposureNotificationApi
import nl.rijksoverheid.en.labtest.LabTestRepository.ScheduleUploadTeksResult.RequireConsent
import nl.rijksoverheid.en.labtest.LabTestRepository.ScheduleUploadTeksResult.Success

class LabTestRepository(
    private val exposureNotificationApi: ExposureNotificationApi
) {
    suspend fun requestKey(): RequestKeyResult {
        delay(1000)
        return RequestKeyResult.Success("A56-34F")
    }

    suspend fun scheduleUploadTeks(): ScheduleUploadTeksResult {
        return when (val result = exposureNotificationApi.requestTemporaryExposureKeyHistory()) {
            is TemporaryExposureKeysResult.RequireConsent -> RequireConsent(result.resolution)
            else -> Success
        }
    }

    sealed class ScheduleUploadTeksResult {
        /**
         * Scheduled successfully or completed immediately
         */
        object Success : ScheduleUploadTeksResult()

        /**
         * Consent is required
         */
        data class RequireConsent(val resolution: PendingIntent) : ScheduleUploadTeksResult()
    }
}
