/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.enapi

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun ExposureNotificationClient.requestEnableNotifications() =
    suspendCoroutine<StartResult> { c ->
        start().apply {
            addOnSuccessListener { c.resume(StartResult.Started) }
            addOnFailureListener {
                if (it is ApiException) {
                    when (it.status.statusCode) {
                        ExposureNotificationStatusCodes.RESOLUTION_REQUIRED -> {
                            if (it.status.hasResolution()) {
                                c.resume(StartResult.ResolutionRequired(it.status.resolution))
                            } else {
                                c.resume(StartResult.UnknownError(it))
                            }
                        }
                        ExposureNotificationStatusCodes.FAILED_ALREADY_STARTED -> c.resume(
                            StartResult.Started
                        )
                        ExposureNotificationStatusCodes.FAILED -> c.resume(
                            StartResult.UnknownError(
                                it
                            )
                        )
                        else -> c.resume(StartResult.UnknownError(it))
                    }
                } else {
                    c.resume(StartResult.UnknownError(it))
                }
            }
        }
    }

suspend fun ExposureNotificationClient.requestDisableNotifications() =
    suspendCoroutine<StopResult> { c ->
        stop().apply {
            addOnSuccessListener { c.resume(StopResult.Stopped) }
            addOnFailureListener {
                if (it is ApiException) {
                    when (it.status.statusCode) {
                        ExposureNotificationStatusCodes.FAILED -> c.resume(
                            StopResult.UnknownError(
                                it
                            )
                        )
                        else -> c.resume(StopResult.UnknownError(it))
                    }
                } else {
                    c.resume(StopResult.UnknownError(it))
                }
            }
        }
    }

suspend fun ExposureNotificationClient.getStatus() = suspendCoroutine<StatusResult> { c ->
    isEnabled.apply {
        addOnSuccessListener { c.resume(if (it) StatusResult.Enabled else StatusResult.Disabled) }
        addOnFailureListener {
            if (it is ApiException) {
                when (it.status.statusCode) {
                    // FIXME this error code is never handled here, FAILED_NOT_SUPPORTED is wrapped in a more generic API status code
                    ExposureNotificationStatusCodes.FAILED_NOT_SUPPORTED -> c.resume(StatusResult.Unavailable)
                    else -> c.resume(StatusResult.UnknownError(it))
                }
            } else {
                c.resume(StatusResult.UnknownError(it))
            }
        }
    }
}

suspend fun ExposureNotificationClient.getTemporaryExposureKeys(): TemporaryExposureKeysResult =
    suspendCoroutine { c ->
        temporaryExposureKeyHistory.apply {
            addOnSuccessListener {
                c.resume(TemporaryExposureKeysResult.Success(it))
            }
            addOnFailureListener {
                val apiException = it as ApiException
                if (apiException.status.hasResolution()) {
                    c.resume(TemporaryExposureKeysResult.RequireConsent(apiException.status.resolution))
                } else {
                    c.resume(TemporaryExposureKeysResult.Error(it))
                }
            }
        }
    }
