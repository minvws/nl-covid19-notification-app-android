/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

private const val DEFAULT_UPDATE_INTERVAL_MINUTES = 240
private const val DEFAULT_DECOY_PROBABILITY = 0.00118
private const val DEFAULT_MIN_REQUEST_SIZE_BYTES = 1800L
private const val DEFAULT_MAX_REQUEST_SIZE_BYTES = 17000L
private const val DEFAULT_APPOINTMENT_PHONE_NUMBER = "0800-1202"
private const val DEFAULT_APPOINTMENT_URL = "https://coronatest.nl/ik-wil-me-laten-testen/online-een-afspraak-maken"
private const val DEACTIVATED = "deactivated"
private const val DEFAULT_SHARE_KEY_URL = "https://www.coronatest.nl"

@JsonClass(generateAdapter = true)
data class AppConfig(
    @Json(name = "androidMinimumVersion") val requiredAppVersionCode: Int = 0,
    @Json(name = "manifestFrequency") val updatePeriodMinutes: Int = DEFAULT_UPDATE_INTERVAL_MINUTES,
    @Json(name = "decoyProbability") val decoyProbability: Double = DEFAULT_DECOY_PROBABILITY,
    @Json(name = "requestMinimumSize") val requestMinimumSize: Long = DEFAULT_MIN_REQUEST_SIZE_BYTES,
    @Json(name = "requestMaximumSize") val requestMaximumSize: Long = DEFAULT_MAX_REQUEST_SIZE_BYTES,
    @Json(name = "coronaMelderDeactivated") val coronaMelderDeactivated: String? = null,
    @Json(name = "appointmentPhoneNumber") val appointmentPhoneNumber: String = DEFAULT_APPOINTMENT_PHONE_NUMBER,
    @Json(name = "shareKeyURL") val shareKeyURL: String = DEFAULT_SHARE_KEY_URL,
    @Json(name = "coronaTestURL") val coronaTestURL: String = DEFAULT_APPOINTMENT_URL,
    @Json(name = "featureFlags") val featureFlags: List<FeatureFlag> = listOf(
        FeatureFlag(
            FeatureFlagOption.INDEPENDENT_KEY_SHARING.id, true
        )
    ),
    @Json(name = "notification") val notification: AppMessage? = null
) {
    val deactivated: Boolean
        get() = coronaMelderDeactivated == DEACTIVATED

    fun hasFeature(featureFlagOption: FeatureFlagOption) = featureFlags.any {
        it.id == featureFlagOption.id && it.featureEnabled
    }
}
