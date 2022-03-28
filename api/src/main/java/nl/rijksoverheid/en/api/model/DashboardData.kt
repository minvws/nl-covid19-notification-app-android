/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

private const val DEFAULT_MORE_INFO_URL = "https://coronadashboard.rijksoverheid.nl/"

@JsonClass(generateAdapter = true)
data class DashboardData(
    @Json(name = "PositiveTestResults") val positiveTestResults: DashboardItem.PositiveTestResults? = null,
    @Json(name = "CoronaMelderUsers") val coronaMelderUsers: DashboardItem.CoronaMelderUsers? = null,
    @Json(name = "HospitalAdmissions") val hospitalAdmissions: DashboardItem.HospitalAdmissions? = null,
    @Json(name = "IcuAdmissions") val icuAdmissions: DashboardItem.IcuAdmissions? = null,
    @Json(name = "VaccinationCoverage") val vaccinationCoverage: DashboardItem.VaccinationCoverage? = null,
    @Json(name = "more_info_url") val moreInfoUrl: String = DEFAULT_MORE_INFO_URL
) {
    val items get() = listOfNotNull(
        positiveTestResults,
        coronaMelderUsers,
        hospitalAdmissions,
        icuAdmissions,
        vaccinationCoverage
    )

    fun getDashboardItem(reference: DashboardItem.Reference): DashboardItem? {
        return when (reference) {
            DashboardItem.Reference.PositiveTestResults -> positiveTestResults
            DashboardItem.Reference.CoronaMelderUsers -> coronaMelderUsers
            DashboardItem.Reference.HospitalAdmissions -> hospitalAdmissions
            DashboardItem.Reference.IcuAdmissions -> icuAdmissions
            DashboardItem.Reference.VaccinationCoverage -> vaccinationCoverage
        }
    }
}

sealed class DashboardItem(
    open val sortingValue: Int,
    open val highlightedValue: GraphValue? = null,
    open val values: List<GraphValue> = emptyList(),
    val reference: Reference,
    open val moreInfoUrl: String = DEFAULT_MORE_INFO_URL,
) : Parcelable {

    enum class Reference {
        PositiveTestResults,
        CoronaMelderUsers,
        HospitalAdmissions,
        IcuAdmissions,
        VaccinationCoverage
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class PositiveTestResults(
        @Json(name = "SortingValue") override val sortingValue: Int,
        @Json(name = "HighlightedValue") override val highlightedValue: GraphValue?,
        @Json(name = "Values") override val values: List<GraphValue>,
        @Json(name = "InfectedMovingAverage") val dailyAverageAmount: Double,
        @Json(name = "InfectedMovingAverageStart") val dailyAverageStart: Long = 0L,
        @Json(name = "InfectedMovingAverageEnd") val dailyAverageEnd: Long = 0L,
        @Json(name = "InfectedPercentage") val confirmedCases: Float
    ) : DashboardItem(sortingValue, highlightedValue, values, Reference.PositiveTestResults)

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class CoronaMelderUsers(
        @Json(name = "SortingValue") override val sortingValue: Int,
        @Json(name = "HighlightedValue") override val highlightedValue: GraphValue?,
        @Json(name = "Values") override val values: List<GraphValue>
    ) : DashboardItem(sortingValue, highlightedValue, values, Reference.CoronaMelderUsers)

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class HospitalAdmissions(
        @Json(name = "SortingValue") override val sortingValue: Int,
        @Json(name = "HighlightedValue") override val highlightedValue: GraphValue?,
        @Json(name = "Values") override val values: List<GraphValue>,
        @Json(name = "HospitalAdmissionMovingAverage") val dailyAverageAmount: Double,
        @Json(name = "HospitalAdmissionMovingAverageStart") val dailyAverageStart: Long = 0L,
        @Json(name = "HospitalAdmissionMovingAverageEnd") val dailyAverageEnd: Long = 0L,
    ) : DashboardItem(sortingValue, highlightedValue, values, Reference.HospitalAdmissions)

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class IcuAdmissions(
        @Json(name = "SortingValue") override val sortingValue: Int,
        @Json(name = "HighlightedValue") override val highlightedValue: GraphValue?,
        @Json(name = "Values") override val values: List<GraphValue>,
        @Json(name = "IcuAdmissionMovingAverage") val dailyAverageAmount: Double,
        @Json(name = "IcuAdmissionMovingAverageStart") val dailyAverageStart: Long = 0L,
        @Json(name = "IcuAdmissionMovingAverageEnd") val dailyAverageEnd: Long = 0L,
    ) : DashboardItem(sortingValue, highlightedValue, values, Reference.IcuAdmissions)

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class VaccinationCoverage(
        @Json(name = "SortingValue") override val sortingValue: Int,
        @Json(name = "BoosterCoverage") val boosterCoverage: BoosterCoverage,
        @Json(name = "BoosterCoverage18Plus") val boosterCoverage18Plus: Float,
        @Json(name = "VaccinationCoverage18Plus") val vaccinationCoverage18Plus: Float,
    ) : DashboardItem(sortingValue, null, boosterCoverage.values, Reference.VaccinationCoverage) {

        @Parcelize
        @JsonClass(generateAdapter = true)
        data class BoosterCoverage(@Json(name = "Values") val values: List<GraphValue>) : Parcelable
    }
}

@Parcelize
@JsonClass(generateAdapter = true)
data class GraphValue(
    @Json(name = "Timestamp") val timestamp: Long,
    @Json(name = "Value") val value: Double
) : Parcelable
