/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.enapi.nearby

import com.google.android.gms.nearby.exposurenotification.DailySummariesConfig
import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import com.google.android.gms.nearby.exposurenotification.ReportType
import com.google.android.gms.nearby.exposurenotification.ScanInstance
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.time.LocalDate
import java.time.ZoneId

@RunWith(JUnit4::class)
class RiskModelTest {

    @Test
    fun `getDailyRiskScores calculate risk score based on a single ExposureWindow`() {
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
        val dateMillisSinceEpoch = date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val daysSinceEpoch = dateMillisSinceEpoch / (1000 * 60 * 60 * 24)
        val riskScores = model.getDailyRiskScores(
            listOf(
                ExposureWindow.Builder().setDateMillisSinceEpoch(dateMillisSinceEpoch)
                    .setInfectiousness(Infectiousness.STANDARD)
                    .setScanInstances(
                        listOf(
                            ScanInstance.Builder()
                                .setSecondsSinceLastScan(600)
                                .setTypicalAttenuationDb(54).build()
                        )
                    )
                    .build()
            )
        )

        assertEquals(1, riskScores.size)
        assertEquals(1200.0, riskScores.first { it.daysSinceEpoch == daysSinceEpoch }.scoreSum)
        assertEquals(1200.0, riskScores.first { it.daysSinceEpoch == daysSinceEpoch }.maximumScore)
    }

    @Test
    fun `getDailyRiskScores calculates max and summed risk score`() {
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
        val dateMillisSinceEpoch = date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val daysSinceEpoch = dateMillisSinceEpoch / (1000 * 60 * 60 * 24)
        val riskScores = model.getDailyRiskScores(
            listOf(
                ExposureWindow.Builder().setDateMillisSinceEpoch(dateMillisSinceEpoch)
                    .setInfectiousness(Infectiousness.STANDARD)
                    .setScanInstances(
                        listOf(
                            ScanInstance.Builder()
                                .setSecondsSinceLastScan(600)
                                .setTypicalAttenuationDb(57).build()
                        )
                    )
                    .build(),
                ExposureWindow.Builder().setDateMillisSinceEpoch(dateMillisSinceEpoch)
                    .setInfectiousness(Infectiousness.STANDARD)
                    .setScanInstances(
                        listOf(
                            ScanInstance.Builder()
                                .setSecondsSinceLastScan(600)
                                .setTypicalAttenuationDb(54).build()
                        )
                    )
                    .build()
            )
        )

        assertEquals(1, riskScores.size)
        assertEquals(1800.0, riskScores.first { it.daysSinceEpoch == daysSinceEpoch }.scoreSum)
        assertEquals(1200.0, riskScores.first { it.daysSinceEpoch == daysSinceEpoch }.maximumScore)
    }

    @Test
    fun `getDailyRiskScores for multiple days`() {
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
        val day1MillisSinceEpoch = date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val day1 = day1MillisSinceEpoch / (1000 * 60 * 60 * 24)
        val day2MillisSinceEpoch = date.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val day2 = day2MillisSinceEpoch / (1000 * 60 * 60 * 24)
        val riskScores = model.getDailyRiskScores(
            listOf(
                ExposureWindow.Builder().setDateMillisSinceEpoch(day1MillisSinceEpoch)
                    .setInfectiousness(Infectiousness.STANDARD)
                    .setScanInstances(
                        listOf(
                            ScanInstance.Builder()
                                .setSecondsSinceLastScan(600)
                                .setTypicalAttenuationDb(54).build()
                        )
                    )
                    .build(),
                ExposureWindow.Builder().setDateMillisSinceEpoch(day2MillisSinceEpoch)
                    .setInfectiousness(Infectiousness.STANDARD)
                    .setScanInstances(
                        listOf(
                            ScanInstance.Builder()
                                .setSecondsSinceLastScan(600)
                                .setTypicalAttenuationDb(57).build()
                        )
                    )
                    .build()
            )
        )

        assertEquals(2, riskScores.size)
        assertEquals(1200.0, riskScores.first { it.daysSinceEpoch == day1 }.scoreSum)
        assertEquals(1200.0, riskScores.first { it.daysSinceEpoch == day1 }.maximumScore)
        assertEquals(600.0, riskScores.first { it.daysSinceEpoch == day2 }.scoreSum)
        assertEquals(600.0, riskScores.first { it.daysSinceEpoch == day2 }.maximumScore)
    }

    @Test
    fun `getDailyRiskScores minimum window scores will be filtered`() {
        val model = RiskModel(
            DailySummariesConfig.DailySummariesConfigBuilder()
                .setMinimumWindowScore(601.0)
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
        val dateMillisSinceEpoch = date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val daysSinceEpoch = dateMillisSinceEpoch / (1000 * 60 * 60 * 24)
        val riskScores = model.getDailyRiskScores(
            listOf(
                ExposureWindow.Builder().setDateMillisSinceEpoch(dateMillisSinceEpoch)
                    .setInfectiousness(Infectiousness.STANDARD)
                    .setScanInstances(
                        listOf(
                            ScanInstance.Builder()
                                .setSecondsSinceLastScan(600)
                                .setTypicalAttenuationDb(54).build()
                        )
                    )
                    .build(),
                ExposureWindow.Builder().setDateMillisSinceEpoch(dateMillisSinceEpoch)
                    .setInfectiousness(Infectiousness.STANDARD)
                    .setScanInstances(
                        listOf(
                            ScanInstance.Builder()
                                .setSecondsSinceLastScan(600)
                                .setTypicalAttenuationDb(57).build()
                        )
                    )
                    .build()
            )
        )

        assertEquals(1, riskScores.size)
        assertEquals(1200.0, riskScores.first { it.daysSinceEpoch == daysSinceEpoch }.scoreSum)
        assertEquals(1200.0, riskScores.first { it.daysSinceEpoch == daysSinceEpoch }.maximumScore)
    }

    @Test
    fun `getDailyRiskScores calculate risk for ExposureWindows with multiple scan instances`() {
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
        val dateMillisSinceEpoch = date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val daysSinceEpoch = dateMillisSinceEpoch / (1000 * 60 * 60 * 24)
        val riskScores = model.getDailyRiskScores(
            listOf(
                ExposureWindow.Builder().setDateMillisSinceEpoch(dateMillisSinceEpoch)
                    .setInfectiousness(Infectiousness.STANDARD)
                    .setScanInstances(
                        listOf(
                            ScanInstance.Builder()
                                .setSecondsSinceLastScan(60)
                                .setTypicalAttenuationDb(68).build(),
                            ScanInstance.Builder()
                                .setSecondsSinceLastScan(60)
                                .setTypicalAttenuationDb(57).build(),
                            ScanInstance.Builder()
                                .setSecondsSinceLastScan(60)
                                .setTypicalAttenuationDb(55).build()
                        )
                    )
                    .build()
            )
        )

        assertEquals(1, riskScores.size)
        assertEquals(210.0, riskScores.first { it.daysSinceEpoch == daysSinceEpoch }.scoreSum)
        assertEquals(210.0, riskScores.first { it.daysSinceEpoch == daysSinceEpoch }.maximumScore)
    }
}
