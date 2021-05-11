/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.notification

import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.en.api.model.ResourceBundle
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
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class PostNotificationViewModelTest {

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
    fun `ExposureNotificationGuidanceArgs being used in guidance`() = runBlocking {
        val postNotificationViewModel = PostNotificationViewModel(
            resourceBundleManager,
            appConfigManager
        )
        val exposureDate = LocalDate.now().minusDays(2)
        val notificationDate = LocalDate.now()
        val fakeGuidance = listOf(ResourceBundle.Guidance.Element.Paragraph("title", "body"))

        Mockito.`when`(
            resourceBundleManager.getExposureNotificationGuidance(exposureDate, notificationDate)
        ).thenReturn(
            fakeGuidance
        )

        postNotificationViewModel.setExposureNotificationGuidanceArgs(
            exposureDate,
            notificationDate
        )

        postNotificationViewModel.guidance.observeForTesting {
            Assert.assertEquals(
                fakeGuidance,
                it.values.first()
            )
        }
    }
}