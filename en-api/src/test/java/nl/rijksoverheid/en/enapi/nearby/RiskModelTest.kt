/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.enapi.nearby

import com.google.android.gms.nearby.exposurenotification.DailySummariesConfig
import com.google.android.gms.nearby.exposurenotification.DailySummary
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import com.google.android.gms.nearby.exposurenotification.ReportType
import com.google.android.gms.nearby.exposurenotification.ScanInstance
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.time.LocalDate
import java.time.ZoneId

@RunWith(JUnit4::class)
class RiskModelTest {

    @Test
    fun `Calculate `() {
        val model = RiskModel(
            DailySummariesConfig.DailySummariesConfigBuilder()
                .setMinimumWindowScore(0.0)
                .setDaysSinceExposureThreshold(10)
                .setAttenuationBuckets(listOf(56, 62, 70), listOf(2.0, 1.0, 0.5, 0.0))
                .setInfectiousnessWeight(Infectiousness.STANDARD, 1.0)
                .setInfectiousnessWeight(Infectiousness.HIGH, 2.0)
                .setReportTypeWeight(ReportType.CONFIRMED_CLINICAL_DIAGNOSIS, 1.0)
                .setReportTypeWeight(ReportType.CONFIRMED_TEST, 1.0)
                .setReportTypeWeight(ReportType.SELF_REPORT, 1.0)
                .build()
        )

        val date = LocalDate.now()
        val riskScores = model.getDailyRiskScores(
            listOf(
                ExposureWindow.Builder().setDateMillisSinceEpoch(
                    date.atStartOfDay(
                        ZoneId.of("UTC")
                    ).toInstant().toEpochMilli()
                ).setInfectiousness(Infectiousness.STANDARD)
                    .setScanInstances(
                        listOf(
                            ScanInstance.Builder()
                                .setSecondsSinceLastScan(600)
                                .setTypicalAttenuationDb(54).build()
                        )
                    )
                    .build()
            ),
            RiskModel.ScoreType.SUM
        )

        Assert.assertEquals(1, riskScores.size)
        Assert.assertEquals(1200, riskScores.size)
    }
}