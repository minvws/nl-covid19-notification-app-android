/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.enapi

import nl.rijksoverheid.en.enapi.nearby.DailyRiskScores

sealed class DailyRiskScoresResult {
    /**
     * Calculated risk scores based on the exposure windows returned from the API
     */
    data class Success(val dailyRiskScores: List<DailyRiskScores>) : DailyRiskScoresResult()

    /**
     * An unexpected API error occurred
     */
    data class UnknownError(val exception: Exception) : DailyRiskScoresResult()
}
