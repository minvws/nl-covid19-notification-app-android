/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.dashboard

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import nl.rijksoverheid.en.api.CacheStrategy
import nl.rijksoverheid.en.api.CdnService
import nl.rijksoverheid.en.api.model.DashboardData
import nl.rijksoverheid.en.util.DashboardServerError
import nl.rijksoverheid.en.util.Resource
import timber.log.Timber

class DashboardRepository(
    private val cdnService: CdnService,
    private val dispatcher: CoroutineDispatcher,
) {

    fun getDashboardData(): Flow<Resource<DashboardData>> = flow {
        emit(Resource.Loading())
        val dashboardData = cdnService.getDashboardData(CacheStrategy.CACHE_LAST)
        emit(Resource.Success(dashboardData))
    }.catch { throwable ->
        Timber.w(throwable)
        emit(Resource.Error(DashboardServerError))
    }.flowOn(dispatcher)
}
