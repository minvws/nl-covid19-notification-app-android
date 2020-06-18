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
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bartoszlipinski.disableanimationsrule.DisableAnimationsRule
import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.ExposureNotificationsViewModel
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.ExposureNotificationService
import nl.rijksoverheid.en.api.model.AppConfig
import nl.rijksoverheid.en.api.model.Manifest
import nl.rijksoverheid.en.api.model.RiskCalculationParameters
import nl.rijksoverheid.en.job.ProcessManifestWorkerScheduler
import nl.rijksoverheid.en.test.FakeExposureNotificationApi
import nl.rijksoverheid.en.test.withFragment
import okhttp3.ResponseBody
import org.junit.Assert
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

@Suppress("UNCHECKED_CAST")
@RunWith(AndroidJUnit4::class)
class HowItWorksFragmentTest {

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

            override suspend fun getManifest(): Manifest =
                Manifest(emptyList(), "", "", "appConfig")

            override suspend fun getRiskCalculationParameters(id: String): RiskCalculationParameters {
                throw NotImplementedError()
            }

            override suspend fun getAppConfig(id: String) = AppConfig(1, 10, 0)
        },
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("${BuildConfig.APPLICATION_ID}.notifications", 0),
        object : ProcessManifestWorkerScheduler {
            override fun schedule(intervalMinutes: Int) {
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
    fun testGotoDetail() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = TestNavHostController(context).apply {
            setGraph(R.navigation.nav_onboarding)
            setCurrentDestination(R.id.nav_how_it_works)
        }
        withFragment(
            HowItWorksFragment(),
            navController,
            R.style.AppTheme,
            activityViewModelFactory
        ) {
            Espresso.onView(withText(R.string.faq_location)).perform(click())

            Assert.assertEquals(
                "Pressing FAQ item navigates to detail page",
                R.id.nav_how_it_works_detail, navController.currentDestination?.id
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
            HowItWorksFragment(),
            navController,
            R.style.AppTheme,
            activityViewModelFactory
        ) {
            Espresso.onView(ViewMatchers.withId(R.id.request)).perform(click())

            Assert.assertEquals(
                "Request permission with success closes the onboarding",
                null, navController.currentDestination?.id
            )
        }
    }
}
