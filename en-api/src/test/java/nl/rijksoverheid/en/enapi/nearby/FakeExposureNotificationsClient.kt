/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
@file:Suppress("DEPRECATION")

package nl.rijksoverheid.en.enapi.nearby

import com.google.android.gms.common.api.Api
import com.google.android.gms.common.api.internal.ApiKey
import com.google.android.gms.nearby.exposurenotification.DailySummariesConfig
import com.google.android.gms.nearby.exposurenotification.DailySummary
import com.google.android.gms.nearby.exposurenotification.DiagnosisKeyFileProvider
import com.google.android.gms.nearby.exposurenotification.DiagnosisKeysDataMapping
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatus
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.PackageConfiguration
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import java.io.File

abstract class FakeExposureNotificationsClient : ExposureNotificationClient {
    override fun isEnabled(): Task<Boolean> = Tasks.forException(IllegalStateException())

    @Suppress("DEPRECATION")
    override fun provideDiagnosisKeys(
        files: List<File>,
        config: ExposureConfiguration,
        token: String
    ): Task<Void> = Tasks.forException(IllegalStateException())

    override fun provideDiagnosisKeys(files: List<File>): Task<Void> =
        Tasks.forException(IllegalStateException())

    override fun provideDiagnosisKeys(provider: DiagnosisKeyFileProvider): Task<Void> =
        Tasks.forException(java.lang.IllegalStateException())

    override fun deviceSupportsLocationlessScanning(): Boolean {
        throw NotImplementedError()
    }

    override fun getExposureWindows(): Task<MutableList<ExposureWindow>> {
        throw NotImplementedError()
    }

    override fun setDiagnosisKeysDataMapping(p0: DiagnosisKeysDataMapping?): Task<Void> {
        throw NotImplementedError()
    }

    override fun getDiagnosisKeysDataMapping(): Task<DiagnosisKeysDataMapping> {
        throw NotImplementedError()
    }

    override fun getCalibrationConfidence(): Task<Int> {
        throw NotImplementedError()
    }

    override fun getVersion(): Task<Long> {
        throw NotImplementedError()
    }

    override fun getDailySummaries(p0: DailySummariesConfig?): Task<MutableList<DailySummary>> {
        throw NotImplementedError()
    }

    @Suppress("DEPRECATION")
    override fun getExposureSummary(token: String): Task<ExposureSummary> =
        Tasks.forException(IllegalStateException())

    override fun start(): Task<Void> = Tasks.forException(IllegalStateException())

    override fun stop(): Task<Void> = Tasks.forException(IllegalStateException())

    @Suppress("DEPRECATION")
    override fun getExposureInformation(token: String): Task<List<ExposureInformation>> =
        Tasks.forException(IllegalStateException())

    override fun getApiKey(): ApiKey<Api.ApiOptions.NoOptions> {
        throw NotImplementedError()
    }

    override fun getTemporaryExposureKeyHistory(): Task<List<TemporaryExposureKey>> =
        Tasks.forException(IllegalStateException())

    override fun getExposureWindows(token: String?): Task<MutableList<ExposureWindow>> =
        Tasks.forException(IllegalStateException())

    override fun getStatus(): Task<MutableSet<ExposureNotificationStatus>> =
        Tasks.forException(IllegalStateException())

    override fun getPackageConfiguration(): Task<PackageConfiguration> =
        Tasks.forException(IllegalStateException())

    override fun requestPreAuthorizedTemporaryExposureKeyHistory(): Task<Void> =
        Tasks.forException(IllegalStateException())

    override fun requestPreAuthorizedTemporaryExposureKeyRelease(): Task<Void> =
        Tasks.forException(IllegalStateException())
}
