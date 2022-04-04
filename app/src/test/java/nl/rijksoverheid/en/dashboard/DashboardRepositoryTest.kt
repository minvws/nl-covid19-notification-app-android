/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.dashboard

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import nl.rijksoverheid.en.api.CacheStrategy
import nl.rijksoverheid.en.api.CdnService
import nl.rijksoverheid.en.api.model.DashboardData
import nl.rijksoverheid.en.dashboardTestData
import nl.rijksoverheid.en.util.DashboardServerError
import nl.rijksoverheid.en.util.Resource
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import retrofit2.HttpException

@RunWith(RobolectricTestRunner::class)
class DashboardRepositoryTest {

    @Mock
    private lateinit var cdnService: CdnService

    @Mock
    private lateinit var httpException: HttpException

    private lateinit var closeable: AutoCloseable

    private val testDispatcher = TestCoroutineDispatcher()

    @Before
    fun openMocks() {
        closeable = MockitoAnnotations.openMocks(this)
    }

    @After
    fun releaseMocks() {
        closeable.close()
    }

    @Test
    fun `getDashboardData returns service call result as Success Resource`() = runBlocking {
        Mockito.`when`(cdnService.getDashboardData(CacheStrategy.CACHE_LAST))
            .thenReturn(dashboardTestData)

        val dashboardRepository = DashboardRepository(cdnService, testDispatcher)

        dashboardRepository.getDashboardData().collect { dashboardResource ->
            if (dashboardResource !is Resource.Loading) {
                Assert.assertTrue(dashboardResource is Resource.Success)
                Assert.assertEquals(dashboardTestData, dashboardResource.data)
            }
        }
    }

    @Test
    fun `getDashboardData from cdnService failed returns DashboardServerError`() = runBlocking {
        Mockito.`when`(cdnService.getDashboardData(CacheStrategy.CACHE_LAST))
            .thenThrow(httpException)

        val dashboardRepository = DashboardRepository(cdnService, testDispatcher)

        dashboardRepository.getDashboardData().collect { dashboardResource ->
            if (dashboardResource !is Resource.Loading) {
                Assert.assertTrue(dashboardResource is Resource.Error)
                Assert.assertEquals(dashboardResource.error?.peekContent(), DashboardServerError)
            }
        }
    }
}