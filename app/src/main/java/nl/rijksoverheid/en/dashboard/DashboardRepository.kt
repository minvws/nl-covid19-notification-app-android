/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.dashboard

import kotlinx.coroutines.coroutineScope
import nl.rijksoverheid.en.api.model.DashboardData
import nl.rijksoverheid.en.api.model.DashboardItem
import nl.rijksoverheid.en.api.model.GraphValue

class DashboardRepository {

    suspend fun getDashboardData(): DashboardData {
        return coroutineScope {
            return@coroutineScope DashboardData(
                DashboardItem.PositiveTestResults(
                    0,
                    GraphValue(1627257600, 54225.0),
                    listOf(
                        GraphValue(1645169766, 50225.0),
                        GraphValue(	1645256166, 53225.0),
                        GraphValue(	1645342566, 51225.0),
                        GraphValue(1645428966, 55225.0),
                        GraphValue(1645518974, 54225.0),
                    ),
                    79530.0,
                    1627257600,
                    1627257800,
                    59.3f
                ),
                DashboardItem.CoronaMelderUsers(
                    1,
                    GraphValue(1627257600, 2650000.0),
                    listOf(
                        GraphValue(1645169766, 281000.0),
                        GraphValue(	1645256166, 2720000.0),
                        GraphValue(	1645342566, 2760000.0),
                        GraphValue(1645428966, 2740000.0),
                        GraphValue(1645518974, 2650000.0),
                    ),
                ),
                DashboardItem.HospitalAdmissions(
                    2,
                    GraphValue(1627257600, 233.0),
                    listOf(
                        GraphValue(1645169766, 230.0),
                        GraphValue(	1645256166, 235.0),
                        GraphValue(	1645342566, 219.0),
                        GraphValue(1645428966, 222.0),
                        GraphValue(1645518974, 233.0),
                    ),
                    142.0,
                    1627257600,
                    1627257800,
                ),
                DashboardItem.IcuAdmissions(
                    3,
                    GraphValue(1647169811, 899.0),
                    listOf(
                        GraphValue(1645169766, 934.0),
                        GraphValue(	1645256166, 913.0),
                        GraphValue(	1645342566, 892.0),
                        GraphValue(1645428966, 899.0),
                        GraphValue(1645518974, 898.0),
                    ),
                    11.0,
                    1627257600,
                    1627257800,
                ),
                DashboardItem.VaccinationCoverage(
                    4,
                    listOf(
                        GraphValue(1645169766, 8652243.0),
                        GraphValue(	1645256166, 8652543.0),
                        GraphValue(	1645342566, 8652943.0),
                        GraphValue(1645428966, 8653043.0),
                        GraphValue(1645518974, 8653243.0),
                    ),
                    59.4f,
                    86.3f
                )
            )
        }
    }
}