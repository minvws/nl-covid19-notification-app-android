/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import nl.rijksoverheid.en.api.model.DashboardData
import nl.rijksoverheid.en.api.model.DashboardItem
import nl.rijksoverheid.en.api.model.GraphValue
import nl.rijksoverheid.en.api.model.MovingAverage

val dashboardTestData = DashboardData(
    positiveTestResults = DashboardItem.PositiveTestResults(
        0,
        GraphValue(1649065335, 10000.0),
        listOf(
            GraphValue(1645065335, 10000.0),
            GraphValue(1646065335, 12000.0),
            GraphValue(1647065335, 1100.0)
        ),
        MovingAverage(1645065335, 1647065335, 500.0),
        1250f
    ),
    coronaMelderUsers = DashboardItem.CoronaMelderUsers(
        0,
        GraphValue(1649065335, 1300000.0),
        listOf(
            GraphValue(1645065335, 1300000.0),
            GraphValue(1646065335, 1290000.0),
            GraphValue(1647065335, 1280000.0)
        )
    )
)
