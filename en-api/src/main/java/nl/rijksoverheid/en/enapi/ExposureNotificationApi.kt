/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
package nl.rijksoverheid.en.enapi

import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import java.io.File

interface ExposureNotificationApi {
    /**
     * Get the status of the exposure notifications api
     * @return the status
     */
    suspend fun getStatus(): StatusResult

    /**
     * Request to enable Exposure Notifications
     * @return the result of the request
     */
    suspend fun requestEnableNotifications(): EnableNotificationsResult

    /**
     * Request to disable exposure notifications
     * @return the result
     */
    suspend fun disableNotifications(): DisableNotificationsResult

    /**
     * Request the tempoary exposure key history
     * @return the result. For the initial call is is most likely [TemporaryExposureKeysResult.RequireConsent]
     */
    suspend fun requestTemporaryExposureKeyHistory(): TemporaryExposureKeysResult

    /**
     * Provide the diagnostics keys for exposure notifications matching
     *
     * @param files the list of files to process. The files will be deleted when processing is successful
     * @param configuration the configuration to use for matching
     * @param token token that will be returned as [ExposureNotificationClient.EXTRA_TOKEN] when a match occurs
     * @return the result
     */
    suspend fun provideDiagnosisKeys(
        files: List<File>,
        configuration: ExposureConfiguration,
        token: String
    ): DiagnosisKeysResult

    /**
     * Get the [ExposureSummary] by token
     * @param token the token passed to [provideDiagnosisKeys] and from [ExposureNotificationClient.EXTRA_TOKEN]
     * @return the summary or null if there's no match or an error occurred
     */
    suspend fun getSummary(token: String): ExposureSummary?

    /**
     * Return whether the device requires location services enabled for BLE scanning
     * @return true if location services do not need to be enabled, false otherwise
     */
    fun deviceSupportsLocationlessScanning(): Boolean
}
