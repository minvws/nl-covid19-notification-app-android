/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.enapi.nearby

import com.google.android.gms.nearby.exposurenotification.DailySummariesConfig
import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import kotlin.math.max

class RiskModel(private val config: DailySummariesConfig) {

    /**
     * Gets the daily list of risk scores from the given exposure windows.
     */
    fun getDailyRiskScores(windows: List<ExposureWindow>, scoreType: ScoreType = ScoreType.MAX): Map<Long, Double> {
        val perDayScore = mutableMapOf<Long, Double>()
        windows.forEach { window ->
            val date = window.dateMillisSinceEpoch
            perDayScore[date] = when(scoreType) {
                ScoreType.SUM -> perDayScore.getOrElse(date, { 0.0 }) + getWindowScore(window)
                ScoreType.MAX -> max(perDayScore.getOrElse(date, { 0.0 }), getWindowScore(window))
            }
        }

        return perDayScore.filterValues {
            it >= config.minimumWindowScore
        }
    }

    /**
     * Computes the risk score associated with a single window based on the exposure
     * seconds, attenuation, and report type.
     */
    fun getWindowScore(window: ExposureWindow): Double {
        val scansScore = window.scanInstances.sumByDouble { scan ->
            scan.secondsSinceLastScan * getAttenuationMultiplier(scan.typicalAttenuationDb)
        }
        return (scansScore * getReportTypeMultiplier(window.reportType)
            * getInfectiousnessMultiplier(window.infectiousness))
    }

    fun getReportTypeMultiplier(reportType: Int): Double {
        return config.reportTypeWeights[reportType] ?: 0.0
    }

    fun getAttenuationMultiplier(attenuationDb: Int): Double {
        val attenuationBucket =  when {
            attenuationDb <= config.attenuationBucketThresholdDb[0] -> 0
            attenuationDb <= config.attenuationBucketThresholdDb[1] -> 1
            attenuationDb <= config.attenuationBucketThresholdDb[2] -> 2
            else -> 3
        }
        return config.attenuationBucketWeights[attenuationBucket]
    }

    fun getInfectiousnessMultiplier(infectiousness: Int): Double {
        return config.infectiousnessWeights[infectiousness] ?: 0.0
    }

    enum class ScoreType {
        SUM,
        MAX
    }

}