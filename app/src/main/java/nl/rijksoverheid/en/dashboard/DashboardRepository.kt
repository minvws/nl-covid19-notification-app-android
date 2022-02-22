/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.dashboard

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import nl.rijksoverheid.en.api.model.DashboardData
import nl.rijksoverheid.en.api.model.DashboardItem
import nl.rijksoverheid.en.api.model.GraphValue

class DashboardRepository {

    suspend fun getDashboardData(): DashboardData {
        return coroutineScope {
            delay(1000)
            return@coroutineScope DashboardData(
                DashboardItem.PositiveTestResults(
                    0,
                    GraphValue(1627257600, 54225.0),
                    listOf(),
                    79530.0,
                    1627257600,
                    1627257800,
                    59.3
                ),
                DashboardItem.CoronaMelderUsers(
                    1,
                    GraphValue(1627257600, 2650000.0),
                    listOf(),
                ),
                DashboardItem.HospitalAdmissions(
                    2,
                    GraphValue(1627257600, 233.0),
                    listOf(),
                    142.0,
                    1627257600,
                    1627257800,
                ),
                DashboardItem.IcuAdmissions(
                    3,
                    GraphValue(1627257600, 898.0),
                    listOf(),
                    11.0,
                    1627257600,
                    1627257800,
                ),
                DashboardItem.VaccinationCoverage(
                    4,
                    listOf(),
                    59.4f,
                    86.3f
                )
            )
        }
    }
}