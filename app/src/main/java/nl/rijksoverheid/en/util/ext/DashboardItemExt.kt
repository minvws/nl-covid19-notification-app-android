/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util.ext

import android.content.Context
import androidx.core.content.ContextCompat
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.model.DashboardItem

val DashboardItem.icon: Int
    get() = when (this) {
        is DashboardItem.PositiveTestResults -> R.drawable.ic_positive_test_results
        is DashboardItem.CoronaMelderUsers -> R.drawable.ic_corona_melder
        is DashboardItem.HospitalAdmissions -> R.drawable.ic_hospital_admissions
        is DashboardItem.IcuAdmissions -> R.drawable.ic_icu_admissions
        is DashboardItem.VaccinationCoverage -> R.drawable.ic_vaccination_coverage
    }

fun DashboardItem.getIconTint(context: Context) =
    if (this !is DashboardItem.CoronaMelderUsers) {
        ContextCompat.getColor(context, R.color.color_primary)
    } else {
        null
    }

val DashboardItem.title: Int
    get() = when (this) {
        is DashboardItem.PositiveTestResults -> R.string.dashboard_positive_test_results_header
        is DashboardItem.CoronaMelderUsers -> R.string.dashboard_corona_melder_users_header
        is DashboardItem.HospitalAdmissions -> R.string.dashboard_hospital_admissions_header
        is DashboardItem.IcuAdmissions -> R.string.dashboard_icu_admissions_header
        is DashboardItem.VaccinationCoverage -> R.string.dashboard_vaccination_coverage_header
    }
