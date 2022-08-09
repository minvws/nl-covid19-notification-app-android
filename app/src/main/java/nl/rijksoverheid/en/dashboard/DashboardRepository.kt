/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.dashboard

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import nl.rijksoverheid.en.api.CacheStrategy
import nl.rijksoverheid.en.api.CdnService
import nl.rijksoverheid.en.api.model.DashboardData
import timber.log.Timber

class DashboardRepository(
    private val cdnService: CdnService
) {
    fun getDashboardData(): Flow<DashboardDataResult> = flow<DashboardDataResult> {
        val dashboardData = cdnService.getDashboardData(CacheStrategy.CACHE_LAST)
        emit(DashboardDataResult.Success(dashboardData))
    }.catch { throwable ->
        Timber.w(throwable)
        emit(DashboardDataResult.Error)
    }
}

sealed class DashboardDataResult {
    data class Success(val data: DashboardData) : DashboardDataResult()
    object Error : DashboardDataResult()
}
