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
import com.google.android.gms.common.api.Api
import com.google.android.gms.common.api.internal.ApiKey
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.ExposureNotificationsViewModel
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.ExposureNotificationService
import nl.rijksoverheid.en.enapi.ExposureNotificationApi
import nl.rijksoverheid.en.test.withFragment
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@Suppress("UNCHECKED_CAST")
@RunWith(AndroidJUnit4::class)
class EnableApiFragmentTest {

    private val repository = ExposureNotificationsRepository(
        ApplicationProvider.getApplicationContext(),
        ExposureNotificationApi(object : FakeExposureNotificationsClient() {}),
        ExposureNotificationService.instance,
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("${BuildConfig.APPLICATION_ID}.notifications", 0)
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

    private abstract class FakeExposureNotificationsClient : ExposureNotificationClient {
        override fun isEnabled(): Task<Boolean> = Tasks.forResult(false)

        override fun provideDiagnosisKeys(
            files: List<File>,
            config: ExposureConfiguration,
            token: String
        ): Task<Void> = Tasks.forException(IllegalStateException())

        override fun getExposureSummary(token: String): Task<ExposureSummary> =
            Tasks.forException(IllegalStateException())

        override fun start(): Task<Void> = Tasks.forResult(null)

        override fun stop(): Task<Void> = Tasks.forException(IllegalStateException())

        override fun getExposureInformation(token: String): Task<List<ExposureInformation>> =
            Tasks.forException(IllegalStateException())

        override fun getApiKey(): ApiKey<Api.ApiOptions.NoOptions>? = null

        override fun getTemporaryExposureKeyHistory(): Task<List<TemporaryExposureKey>> =
            Tasks.forException(IllegalStateException())
    }
}
