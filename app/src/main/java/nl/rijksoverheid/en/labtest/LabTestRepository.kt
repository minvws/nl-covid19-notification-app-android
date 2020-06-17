/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest

import android.app.PendingIntent
import android.content.SharedPreferences
import kotlinx.coroutines.delay
import nl.rijksoverheid.en.api.ExposureNotificationService
import nl.rijksoverheid.en.enapi.TemporaryExposureKeysResult
import nl.rijksoverheid.en.enapi.nearby.ExposureNotificationApi

class LabTestRepository(
    private val preferences: SharedPreferences,
    private val exposureNotificationApi: ExposureNotificationApi,
    private val api: ExposureNotificationService,
) {
    suspend fun requestKey(): RequestKeyResult {
        delay(1000)
        return RequestKeyResult.Success("A56-34F")
    }

    suspend fun scheduleUploadTeks(): ScheduleUploadTeksResult {
        return when (val result = exposureNotificationApi.requestTemporaryExposureKeyHistory()) {
            is TemporaryExposureKeysResult.RequireConsent -> ScheduleUploadTeksResult.RequireConsent(result.resolution)
            else -> ScheduleUploadTeksResult.Success
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
