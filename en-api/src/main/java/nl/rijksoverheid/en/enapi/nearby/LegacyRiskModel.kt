/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.enapi.nearby

import android.annotation.SuppressLint
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId

class LegacyRiskModel(private val configuration: ExposureConfiguration) {
    @SuppressLint("NewApi")
    fun getSummary(exposureWindows: List<ExposureWindow>): ExposureSummary {
        val exposuresPerDay = exposureWindows.groupBy { it.dateMillisSinceEpoch }
        Timber.d("getSummary for ${exposureWindows.size} exposure windows, ${exposuresPerDay.size} days")
        val days = exposuresPerDay.keys.sorted().reversed()
        val now = LocalDate.now(ZoneId.of("UTC"))
        var highestSummary = ExposureSummary.ExposureSummaryBuilder().build()
        for (day in days) {
            val windows = exposuresPerDay[day] ?: emptyList()
            for (window in windows) {
                val date = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(window.dateMillisSinceEpoch),
                    ZoneId.of("UTC")
                ).toLocalDate()
                Timber.d("Checking $date (${window.scanInstances.size} scan instances)")
                Timber.d("Scan instances: ${window.scanInstances.joinToString(",") { "a: ${it.typicalAttenuationDb} d: ${it.secondsSinceLastScan}" }}")

                val averageAttenuation =
                    window.scanInstances.map { it.typicalAttenuationDb }.average().toInt()
                val attenuationBucket = attenuationToBucket(averageAttenuation)
                val duration = window.scanInstances.sumBy { it.secondsSinceLastScan }
                val durationBucket = durationToBucket(duration)

                val transmissionRiskBucket = when (window.infectiousness) {
                    Infectiousness.HIGH -> 2
                    Infectiousness.STANDARD -> 1
                    else -> 0
                }

                val score = configuration.attenuationScores[attenuationBucket] *
                    configuration.durationScores[durationBucket] *
                    configuration.transmissionRiskScores[transmissionRiskBucket]

                Timber.d("Attenuation bucket: $attenuationBucket ($averageAttenuation Dbm)")
                Timber.d("Duration bucket: $durationBucket ($duration)")
                Timber.d("Transmission risk bucket: $transmissionRiskBucket")
                Timber.d("Final score $score")

                val summary = ExposureSummary.ExposureSummaryBuilder().setMaximumRiskScore(score)
                    .setMatchedKeyCount(exposureWindows.size)
                    .setSummationRiskScore(score)
                    .setDaysSinceLastExposure(Period.between(date, now).days)
                    /* more stuff */
                    .build()

                if (summary.maximumRiskScore >= configuration.minimumRiskScore) {
                    return summary
                }

                if (summary.maximumRiskScore > highestSummary.maximumRiskScore) {
                    highestSummary = summary
                }
            }
        }
        return highestSummary
    }

    private fun attenuationToBucket(attenuationDb: Int): Int =
        when {
            attenuationDb > 73 -> 0
            attenuationDb in 64..73 -> 1
            attenuationDb in 52..63 -> 2
            attenuationDb in 34..51 -> 3
            attenuationDb in 28..33 -> 4
            attenuationDb in 16..27 -> 5
            attenuationDb in 11..15 -> 6
            else -> 7
        }

    private fun durationToBucket(durationSeconds: Int): Int =
        when {
            durationSeconds == 0 -> 0
            durationSeconds <= 5 * 60 -> 1
            durationSeconds in 6 * 60..10 * 60 -> 2
            durationSeconds in 11 * 60..15 * 60 -> 3
            durationSeconds in 15 * 60..20 * 60 -> 4
            durationSeconds in 21 * 60..25 * 60 -> 5
            durationSeconds in 26 * 60..30 * 60 -> 6
            else -> 7
        }
}
