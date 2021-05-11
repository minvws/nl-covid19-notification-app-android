/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.onboarding

import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.util.observeForTesting
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingViewModelTest {

    @Mock
    private lateinit var onboardingRepository: OnboardingRepository

    @Mock
    private lateinit var exposureNotificationsRepository: ExposureNotificationsRepository

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
    fun `finishOnboarding updates completed state in onboardingRepository`() {
        val onboardingViewModel = OnboardingViewModel(
            onboardingRepository,
            exposureNotificationsRepository
        )

        onboardingViewModel.togglePrivacyPolicyConsent()
        onboardingViewModel.finishOnboarding()

        verify(onboardingRepository, times(1)).setHasCompletedOnboarding(true)
    }

    @Test
    fun `finishOnboarding triggers onboardingComplete event`() {
        val onboardingViewModel = OnboardingViewModel(
            onboardingRepository,
            exposureNotificationsRepository
        )

        onboardingViewModel.togglePrivacyPolicyConsent()
        onboardingViewModel.finishOnboarding()

        onboardingViewModel.onboardingComplete.observeForTesting {
            Assert.assertEquals(
                1,
                it.values.size
            )
        }
    }

    @Test
    fun `privacyPolicyConsentGiven is required to complete onboarding`() {
        val onboardingViewModel = OnboardingViewModel(
            onboardingRepository,
            exposureNotificationsRepository
        )

        onboardingViewModel.finishOnboarding()

        verify(onboardingRepository, never()).setHasCompletedOnboarding(true)
    }

    @Test
    fun `skipConsent triggers skipConsentConfirmation event`() {
        val onboardingViewModel = OnboardingViewModel(
            onboardingRepository,
            exposureNotificationsRepository
        )

        onboardingViewModel.skipConsent()

        onboardingViewModel.skipConsentConfirmation.observeForTesting {
            Assert.assertEquals(
                1,
                it.values.size
            )
        }
    }

    @Test
    fun `privacyPolicyConsentGiven is default false`() {
        val onboardingViewModel = OnboardingViewModel(
            onboardingRepository,
            exposureNotificationsRepository
        )

        onboardingViewModel.privacyPolicyConsentGiven.observeForTesting {
            Assert.assertFalse(
                it.values.first()
            )
        }
    }

    @Test
    fun `continueOnboarding triggers continueOnboarding event`() {
        val onboardingViewModel = OnboardingViewModel(
            onboardingRepository,
            exposureNotificationsRepository
        )

        onboardingViewModel.continueOnboarding()

        onboardingViewModel.continueOnboarding.observeForTesting {
            Assert.assertEquals(
                1,
                it.values.size
            )
        }
    }

    @Test
    fun `isExposureNotificationApiUpToDate false when GooglePlayServices isn't up to date`() {
        val onboardingViewModel = OnboardingViewModel(
            onboardingRepository,
            exposureNotificationsRepository
        )

        runBlocking {
            Mockito.`when`(onboardingRepository.isGooglePlayServicesUpToDate())
                .thenReturn(false)
            Mockito.`when`(exposureNotificationsRepository.isExposureNotificationApiUpdateRequired())
                .thenReturn(false)
            onboardingViewModel.refreshExposureNotificationApiUpToDate()

            onboardingViewModel.isExposureNotificationApiUpToDate.observeForTesting {
                Assert.assertFalse(
                    it.values.first()
                )
            }
        }
    }

    @Test
    fun `isExposureNotificationApiUpToDate false when EN APi requires an update`() {
        val onboardingViewModel = OnboardingViewModel(
            onboardingRepository,
            exposureNotificationsRepository
        )

        runBlocking {
            Mockito.`when`(onboardingRepository.isGooglePlayServicesUpToDate())
                .thenReturn(true)
            Mockito.`when`(exposureNotificationsRepository.isExposureNotificationApiUpdateRequired())
                .thenReturn(true)
            onboardingViewModel.refreshExposureNotificationApiUpToDate()

            onboardingViewModel.isExposureNotificationApiUpToDate.observeForTesting {
                Assert.assertFalse(
                    it.values.first()
                )
            }
        }
    }

    @Test
    fun `isExposureNotificationApiUpToDate is up to date`() {
        val onboardingViewModel = OnboardingViewModel(
            onboardingRepository,
            exposureNotificationsRepository
        )

        runBlocking {
            Mockito.`when`(onboardingRepository.isGooglePlayServicesUpToDate())
                .thenReturn(true)
            Mockito.`when`(exposureNotificationsRepository.isExposureNotificationApiUpdateRequired())
                .thenReturn(false)
            onboardingViewModel.refreshExposureNotificationApiUpToDate()

            onboardingViewModel.isExposureNotificationApiUpToDate.observeForTesting {
                Assert.assertTrue(
                    it.values.first()
                )
            }
        }
    }
}
