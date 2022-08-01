/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.enapi

import com.google.android.gms.nearby.exposurenotification.DailySummariesConfig
import com.google.android.gms.nearby.exposurenotification.DiagnosisKeysDataMapping
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
     * @param diagnosisKeysDataMapping
     * @return the result
     */
    suspend fun provideDiagnosisKeys(
        files: List<File>,
        diagnosisKeysDataMapping: DiagnosisKeysDataMapping
    ): DiagnosisKeysResult

    /**
     * Return a list of DailyRiskScores objects corresponding to the last 14 days of exposure data or null if there's no match or an error occurred
     * @param config which must contain the weights and thresholds to apply to the exposure data
     * @return the result which can contain the risk scores or an exception
     */
    suspend fun getDailyRiskScores(config: DailySummariesConfig): DailyRiskScoresResult

    /**
     * Return whether the device requires location services enabled for BLE scanning
     * @return true if location services do not need to be enabled, false otherwise
     */
    fun deviceSupportsLocationlessScanning(): Boolean

    /**
     * Check if the installed version of ExposureNotification API is at least the required version
     * @return the result which can be UpToDate, RequiresAnUpdate or UnknownError
     */
    suspend fun isExposureNotificationApiUpToDate(): UpdateToDateResult
}
