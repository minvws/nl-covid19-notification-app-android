/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bartoszlipinski.disableanimationsrule.DisableAnimationsRule
import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.ExposureNotificationsViewModel
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.ExposureNotificationService
import nl.rijksoverheid.en.api.model.Manifest
import nl.rijksoverheid.en.api.model.RiskCalculationParameters
import nl.rijksoverheid.en.job.ProcessManifestWorkerScheduler
import nl.rijksoverheid.en.test.FakeExposureNotificationApi
import nl.rijksoverheid.en.test.withFragment
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

@Suppress("UNCHECKED_CAST")
@RunWith(AndroidJUnit4::class)
class EnableApiFragmentTest {

    companion object {
        @ClassRule
        @JvmField
        val disableAnimationsRule: DisableAnimationsRule = DisableAnimationsRule()
    }

    private val repository = ExposureNotificationsRepository(
        ApplicationProvider.getApplicationContext(),
        FakeExposureNotificationApi(),
        object : ExposureNotificationService {
            override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
                throw NotImplementedError()
            }

            override suspend fun getManifest(): Manifest {
                throw NotImplementedError()
            }

            override suspend fun getRiskCalculationParameters(id: String): RiskCalculationParameters {
                throw NotImplementedError()
            }
        },
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("${BuildConfig.APPLICATION_ID}.notifications", 0),
        object : ProcessManifestWorkerScheduler {
            override fun schedule(intervalHours: Int) {
            }

            override fun cancel() {
            }
        }
    )
    private val viewModel = ExposureNotificationsViewModel(repository)
    private val activityViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return viewModel as T
        }
    }

    @Test
    fun testExplanation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = TestNavHostController(context).apply {
            setGraph(R.navigation.nav_onboarding)
            setCurrentDestination(R.id.nav_enable_api)
        }
        withFragment(
            EnableApiFragment(),
            navController,
            R.style.AppTheme,
            activityViewModelFactory
        ) {
            onView(ViewMatchers.withId(R.id.explanation)).perform(click())

            assertEquals(
                "Explanation button navigates to how it works screen",
                R.id.nav_how_it_works, navController.currentDestination?.id
            )
        }
    }

    @Test
    fun testRequest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = TestNavHostController(context).apply {
            setGraph(R.navigation.nav_onboarding)
            setCurrentDestination(R.id.nav_enable_api)
        }
        withFragment(
            EnableApiFragment(),
            navController,
            R.style.AppTheme,
            activityViewModelFactory
        ) {
            onView(ViewMatchers.withId(R.id.request)).perform(click())

            assertEquals(
                "Request permission with success closes the onboarding",
                null, navController.currentDestination?.id
            )
        }
    }

    @Test
    fun testSkip() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = TestNavHostController(context).apply {
            setGraph(R.navigation.nav_onboarding)
            setCurrentDestination(R.id.nav_enable_api)
        }
        withFragment(
            EnableApiFragment(),
            navController,
            R.style.AppTheme,
            activityViewModelFactory
        ) {
            onView(ViewMatchers.withId(R.id.skip)).perform(click())

            assertEquals(
                "Skip button closes the onboarding",
                null, navController.currentDestination?.id
            )
        }
    }
}
