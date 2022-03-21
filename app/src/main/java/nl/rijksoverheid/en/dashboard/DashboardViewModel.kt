/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import nl.rijksoverheid.en.api.model.DashboardData
import nl.rijksoverheid.en.util.Resource

class DashboardViewModel(
    dashboardRepository: DashboardRepository
): ViewModel() {

    val dashboardData: LiveData<Resource<DashboardData>> = dashboardRepository.getDashboardData().asLiveData(
        viewModelScope.coroutineContext
    )
}