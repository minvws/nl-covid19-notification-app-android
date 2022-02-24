/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DashboardData (
    @Json(name = "positive_test_results") val positiveTestResults: DashboardItem.PositiveTestResults? = null,
    @Json(name = "corona_melder_users") val coronaMelderUsers: DashboardItem.CoronaMelderUsers? = null,
    @Json(name = "hospital_admissions") val hospitalAdmissions: DashboardItem.HospitalAdmissions? = null,
    @Json(name = "icu_admissions") val icuAdmissions: DashboardItem.IcuAdmissions? = null,
    @Json(name = "vaccination_coverage") val vaccinationCoverage: DashboardItem.VaccinationCoverage? = null,
) {
    val items get() = listOfNotNull(
        positiveTestResults,
        coronaMelderUsers,
        hospitalAdmissions,
        icuAdmissions,
        vaccinationCoverage
    )
}

sealed class DashboardItem (
    val sortingValue: Int,
    val highlightedValue: GraphValue? = null,
    val values: List<GraphValue> = emptyList()
) {
    @JsonClass(generateAdapter = true)
    class PositiveTestResults(
        @Json(name = "sorting_value") sortingValue: Int,
        @Json(name = "highlighted_value") highlightedValue: GraphValue?,
        @Json(name = "values") values: List<GraphValue>,
        @Json(name = "daily_average_amount") val dailyAverageAmount: Double,
        @Json(name = "daily_average_start") val dailyAverageStart: Long,
        @Json(name = "daily_average_end") val dailyAverageEnd: Long,
        @Json(name = "confirmed_cases") val confirmedCases: Double
    ): DashboardItem(sortingValue, highlightedValue, values)

    @JsonClass(generateAdapter = true)
    class CoronaMelderUsers(
        @Json(name = "sorting_value") sortingValue: Int,
        @Json(name = "highlighted_value") highlightedValue: GraphValue?,
        @Json(name = "values") values: List<GraphValue>
    ): DashboardItem(sortingValue, highlightedValue, values)

    @JsonClass(generateAdapter = true)
    class HospitalAdmissions(
        @Json(name = "sorting_value") sortingValue: Int,
        @Json(name = "highlighted_value") highlightedValue: GraphValue?,
        @Json(name = "values") values: List<GraphValue>,
        @Json(name = "daily_average_amount") val dailyAverageAmount: Double,
        @Json(name = "daily_average_start") val dailyAverageStart: Long,
        @Json(name = "daily_average_end") val dailyAverageEnd: Long,
    ): DashboardItem(sortingValue, highlightedValue, values)

    @JsonClass(generateAdapter = true)
    class IcuAdmissions(
        @Json(name = "sorting_value") sortingValue: Int,
        @Json(name = "highlighted_value") highlightedValue: GraphValue?,
        @Json(name = "values") values: List<GraphValue>,
        @Json(name = "daily_average_amount") val dailyAverageAmount: Double,
        @Json(name = "daily_average_start") val dailyAverageStart: Long,
        @Json(name = "daily_average_end") val dailyAverageEnd: Long,
    ): DashboardItem(sortingValue, highlightedValue, values)

    @JsonClass(generateAdapter = true)
    class VaccinationCoverage(
        @Json(name = "sorting_value") sortingValue: Int,
        @Json(name = "values") values: List<GraphValue>,
        @Json(name = "booster_coverage") val boosterCoverage: Float,
        @Json(name = "elder_coverage") val elderCoverage: Float,
    ): DashboardItem(sortingValue, null, values)
}

@JsonClass(generateAdapter = true)
data class GraphValue (
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "value") val value: Double
)