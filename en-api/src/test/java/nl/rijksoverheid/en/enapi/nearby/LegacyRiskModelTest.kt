/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.enapi.nearby

import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import com.google.android.gms.nearby.exposurenotification.ScanInstance
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.time.LocalDate
import java.time.ZoneId

@RunWith(JUnit4::class)
class LegacyRiskModelTest {

    @Test
    fun `windows are converted to ExposureSummary`() {
        val model = LegacyRiskModel(
            ExposureConfiguration.ExposureConfigurationBuilder()
                .setAttenuationScores(1, 2, 3, 4, 5, 6, 7, 8)
                .setDurationScores(0, 0, 0, 1, 2, 2, 2, 2)
                .setTransmissionRiskScores(0, 2, 2, 2, 0, 0, 0, 0)
                .setMinimumRiskScore(6)
                .build()
        )

        val date = LocalDate.now()
        val summary = model.getSummary(
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
                                .setTypicalAttenuationDb(80).build()
                        )
                    )
                    .build()
            )
        )

        assertEquals(8, summary.maximumRiskScore)
    }
}