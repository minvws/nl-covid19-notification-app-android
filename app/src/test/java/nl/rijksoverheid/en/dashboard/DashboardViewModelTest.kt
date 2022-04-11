/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.dashboard

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.en.dashboardTestData
import nl.rijksoverheid.en.util.Resource
import nl.rijksoverheid.en.util.observeForTesting
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DashboardViewModelTest {

    @Mock
    private lateinit var dashboardRepository: DashboardRepository

    private lateinit var closeable: AutoCloseable

    @Before
    fun openMocks() {
        closeable = MockitoAnnotations.openMocks(this)
    }

    @After
    fun releaseMocks() {
        closeable.close()
    }

    @Test
    fun `verify load dashboardData on init`() = runBlocking {
        val dashboardResource = Resource.Success(dashboardTestData)

        Mockito.`when`(dashboardRepository.getDashboardData())
            .thenReturn(flowOf(dashboardResource))

        val dashboardViewModel = DashboardViewModel(dashboardRepository)

        verify(dashboardRepository, times(1)).getDashboardData()

        dashboardViewModel.dashboardData.observeForTesting {
            Assert.assertEquals(
                dashboardResource,
                it.values.first()
            )
        }
    }
}
