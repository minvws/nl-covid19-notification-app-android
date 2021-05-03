/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.enapi.nearby

import com.google.android.gms.nearby.exposurenotification.DailySummariesConfig
import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import timber.log.Timber
import kotlin.math.max

class RiskModel(private val config: DailySummariesConfig) {

    /**
     * Gets the daily list of risk scores from the given exposure windows.
     */
    fun getDailyRiskScores(windows: List<ExposureWindow>): List<DailyRiskScores> {
        val perDayScore = mutableMapOf<Long, DailyRiskScores>()
        windows.forEach { window ->
            val daysSinceEpoch = window.dateMillisSinceEpoch / (1000 * 60 * 60 * 24)
            val windowScore = getWindowScore(window)
            Timber.d("ExposureWindow=$window, WindowScore=$windowScore, DaysSinceEpoch=$daysSinceEpoch")
            if (windowScore >= config.minimumWindowScore) {
                val dailyRiskScores = perDayScore.getOrElse(daysSinceEpoch) {
                    DailyRiskScores(daysSinceEpoch, 0.0, 0.0)
                }
                perDayScore[daysSinceEpoch] = dailyRiskScores.copy(
                    maximumScore = max(dailyRiskScores.maximumScore, windowScore),
                    scoreSum = dailyRiskScores.scoreSum + windowScore
                )
            }
        }

        return perDayScore.values.toList()
    }

    /**
     * Computes the risk score associated with a single window based on the exposure
     * seconds, attenuation, and report type.
     */
    private fun getWindowScore(window: ExposureWindow): Double {
        val scansScore = window.scanInstances.sumByDouble { scan ->
            scan.secondsSinceLastScan * getAttenuationMultiplier(scan.typicalAttenuationDb)
        }
        return (
            scansScore * getReportTypeMultiplier(window.reportType) *
                getInfectiousnessMultiplier(window.infectiousness)
            )
    }

    private fun getReportTypeMultiplier(reportType: Int): Double {
        return config.reportTypeWeights[reportType] ?: 0.0
    }

    private fun getAttenuationMultiplier(attenuationDb: Int): Double {
        val attenuationBucket = when {
            attenuationDb <= config.attenuationBucketThresholdDb[0] -> 0
            attenuationDb <= config.attenuationBucketThresholdDb[1] -> 1
            attenuationDb <= config.attenuationBucketThresholdDb[2] -> 2
            else -> 3
        }
        return config.attenuationBucketWeights[attenuationBucket]
    }

    private fun getInfectiousnessMultiplier(infectiousness: Int): Double {
        return config.infectiousnessWeights[infectiousness] ?: 0.0
    }
}
