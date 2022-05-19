/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.applifecycle

import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.en.api.model.AppConfig
import nl.rijksoverheid.en.config.AppConfigManager
import nl.rijksoverheid.en.resource.ResourceBundleManager
import nl.rijksoverheid.en.util.observeForTesting
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EndOfLifeViewModelTest {

    @Mock
    private lateinit var resourceBundleManager: ResourceBundleManager

    @Mock
    private lateinit var appConfigManager: AppConfigManager

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
    fun `endOfLifeContent is loaded on init`() {
        val coronaMelderDeactivatedTitle = "end_of_life_title"
        val coronaMelderDeactivatedBody = "end_of_life_body"
        val endOfLifeTitle = "Title"
        val endOfLifeBody = "Body"

        val endOfLifeViewModel = EndOfLifeViewModel(resourceBundleManager, appConfigManager)

        runBlocking {
            Mockito.`when`(appConfigManager.getCachedConfigOrDefault())
                .thenReturn(
                    AppConfig(
                        coronaMelderDeactivated = "deactivated",
                        coronaMelderDeactivatedTitle = coronaMelderDeactivatedTitle,
                        coronaMelderDeactivatedBody = coronaMelderDeactivatedBody
                    )
                )
            Mockito.`when`(resourceBundleManager.getEndOfLifeResources(coronaMelderDeactivatedTitle, coronaMelderDeactivatedBody))
                .thenReturn(Pair(endOfLifeTitle, endOfLifeBody))

            endOfLifeViewModel.endOfLifeContent.observeForTesting {
                Assert.assertEquals(
                    listOf(Pair(endOfLifeTitle, endOfLifeBody)),
                    it.values
                )
            }
        }
    }
}
