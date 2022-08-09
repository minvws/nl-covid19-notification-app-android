/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.dashboard

import kotlinx.coroutines.test.runTest
import nl.rijksoverheid.en.api.CacheStrategy
import nl.rijksoverheid.en.api.CdnService
import nl.rijksoverheid.en.dashboardTestData
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations.openMocks
import org.robolectric.RobolectricTestRunner
import retrofit2.HttpException

@RunWith(RobolectricTestRunner::class)
class DashboardRepositoryTest {

    @Mock
    private lateinit var cdnService: CdnService

    @Mock
    private lateinit var httpException: HttpException

    private lateinit var closeable: AutoCloseable

    @Before
    fun openMocks() {
        closeable = openMocks(this)
    }

    @After
    fun releaseMocks() {
        closeable.close()
    }

    @Test
    fun `getDashboardData returns success result`() = runTest {
        `when`(cdnService.getDashboardData(CacheStrategy.CACHE_LAST))
            .thenReturn(dashboardTestData)

        val dashboardRepository = DashboardRepository(cdnService)

        dashboardRepository.getDashboardData().collect { dashboardResource ->
            assertTrue(dashboardResource is DashboardDataResult.Success)
            assertEquals(dashboardTestData, (dashboardResource as DashboardDataResult.Success).data)
        }
    }

    @Test
    fun `getDashboardData from cdnService failed returns Error`() = runTest {
        `when`(cdnService.getDashboardData(CacheStrategy.CACHE_LAST))
            .thenThrow(httpException)

        val dashboardRepository = DashboardRepository(cdnService)

        dashboardRepository.getDashboardData().collect { dashboardResource ->
            assertTrue(dashboardResource is DashboardDataResult.Error)
        }
    }
}
