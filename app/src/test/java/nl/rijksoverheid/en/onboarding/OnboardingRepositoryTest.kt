/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.onboarding

import android.app.Application
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingRepositoryTest {

    private lateinit var sharedPreferences: SharedPreferences

    @Before
    fun setup() {
        sharedPreferences = ApplicationProvider.getApplicationContext<Application>()
            .getSharedPreferences("onboarding_repository_test", 0)
    }

    @Test
    fun `hasCompletedOnboarding return true after calling setHasCompletedOnboarding`() {
        runBlocking {
            val onboardingRepository = OnboardingRepository(
                sharedPreferences = sharedPreferences,
                googlePlayServicesUpToDateChecker = { true }
            )

            Assert.assertFalse(onboardingRepository.hasCompletedOnboarding())

            onboardingRepository.setHasCompletedOnboarding(true)

            Assert.assertTrue(onboardingRepository.hasCompletedOnboarding())
        }
    }
}
