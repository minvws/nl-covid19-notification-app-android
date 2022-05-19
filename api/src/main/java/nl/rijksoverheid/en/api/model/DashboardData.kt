/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

private const val DEFAULT_MORE_INFO_URL = "https://coronadashboard.rijksoverheid.nl/"

@JsonClass(generateAdapter = true)
data class DashboardData(
    @Json(name = "positiveTestResults") val positiveTestResults: DashboardItem.PositiveTestResults? = null,
    @Json(name = "coronaMelderUsers") val coronaMelderUsers: DashboardItem.CoronaMelderUsers? = null,
    @Json(name = "hospitalAdmissions") val hospitalAdmissions: DashboardItem.HospitalAdmissions? = null,
    @Json(name = "icuAdmissions") val icuAdmissions: DashboardItem.IcuAdmissions? = null,
    @Json(name = "vaccinationCoverage") val vaccinationCoverage: DashboardItem.VaccinationCoverage? = null,
    @Json(name = "moreInfoUrl") val moreInfoUrl: String = DEFAULT_MORE_INFO_URL
) {
    val items get() = listOfNotNull(
        positiveTestResults,
        coronaMelderUsers,
        hospitalAdmissions,
        icuAdmissions,
        vaccinationCoverage
    )

    fun getDashboardItem(reference: DashboardItemRef): DashboardItem? {
        return when (reference) {
            DashboardItemRef.PositiveTestResults -> positiveTestResults
            DashboardItemRef.CoronaMelderUsers -> coronaMelderUsers
            DashboardItemRef.HospitalAdmissions -> hospitalAdmissions
            DashboardItemRef.IcuAdmissions -> icuAdmissions
            DashboardItemRef.VaccinationCoverage -> vaccinationCoverage
        }
    }
}

sealed class DashboardItem(
    open val sortingValue: Int,
    open val highlightedValue: GraphValue? = null,
    open val values: List<GraphValue> = emptyList(),
    val reference: DashboardItemRef,
    open val moreInfoUrl: String = DEFAULT_MORE_INFO_URL,
) {

    @JsonClass(generateAdapter = true)
    data class PositiveTestResults(
        @Json(name = "sortingValue") override val sortingValue: Int,
        @Json(name = "highlightedValue") override val highlightedValue: GraphValue?,
        @Json(name = "values") override val values: List<GraphValue>,
        @Json(name = "infectedMovingAverage") val infectedMovingAverage: MovingAverage,
        @Json(name = "infectedPercentage") val confirmedCases: Float
    ) : DashboardItem(sortingValue, highlightedValue, values, DashboardItemRef.PositiveTestResults)

    @JsonClass(generateAdapter = true)
    data class CoronaMelderUsers(
        @Json(name = "sortingValue") override val sortingValue: Int,
        @Json(name = "highlightedValue") override val highlightedValue: GraphValue?,
        @Json(name = "values") override val values: List<GraphValue>
    ) : DashboardItem(sortingValue, highlightedValue, values, DashboardItemRef.CoronaMelderUsers)

    @JsonClass(generateAdapter = true)
    data class HospitalAdmissions(
        @Json(name = "sortingValue") override val sortingValue: Int,
        @Json(name = "highlightedValue") override val highlightedValue: GraphValue?,
        @Json(name = "values") override val values: List<GraphValue>,
        @Json(name = "hospitalAdmissionMovingAverage") val hospitalAdmissionMovingAverage: MovingAverage,
    ) : DashboardItem(sortingValue, highlightedValue, values, DashboardItemRef.HospitalAdmissions)

    @JsonClass(generateAdapter = true)
    data class IcuAdmissions(
        @Json(name = "sortingValue") override val sortingValue: Int,
        @Json(name = "highlightedValue") override val highlightedValue: GraphValue?,
        @Json(name = "values") override val values: List<GraphValue>,
        @Json(name = "icuAdmissionMovingAverage") val icuAdmissionMovingAverage: MovingAverage,
    ) : DashboardItem(sortingValue, highlightedValue, values, DashboardItemRef.IcuAdmissions)

    @JsonClass(generateAdapter = true)
    data class VaccinationCoverage(
        @Json(name = "sortingValue") override val sortingValue: Int,
        @Json(name = "boosterCoverage") val boosterCoverage: BoosterCoverage = BoosterCoverage(emptyList()),
        @Json(name = "boosterCoverage18Plus") val boosterCoverage18Plus: Float,
        @Json(name = "vaccinationCoverage18Plus") val vaccinationCoverage18Plus: Float,
    ) : DashboardItem(sortingValue, null, boosterCoverage.values, DashboardItemRef.VaccinationCoverage) {

        @JsonClass(generateAdapter = true)
        data class BoosterCoverage(@Json(name = "Values") val values: List<GraphValue>)
    }
}

enum class DashboardItemRef {
    PositiveTestResults,
    CoronaMelderUsers,
    HospitalAdmissions,
    IcuAdmissions,
    VaccinationCoverage
}

@JsonClass(generateAdapter = true)
data class GraphValue(
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "value") val value: Double
)

@JsonClass(generateAdapter = true)
data class MovingAverage(
    @Json(name = "timestampStart") val timestampStart: Long,
    @Json(name = "timestampEnd") val timestampEnd: Long,
    @Json(name = "value") val value: Double
)
