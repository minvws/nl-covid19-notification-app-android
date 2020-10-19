/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.onboarding

import android.content.Context
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bartoszlipinski.disableanimationsrule.DisableAnimationsRule
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import nl.rijksoverheid.en.BaseInstrumentationTest
import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.ExposureNotificationsViewModel
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.CdnService
import nl.rijksoverheid.en.api.model.AppConfig
import nl.rijksoverheid.en.api.model.Manifest
import nl.rijksoverheid.en.api.model.RiskCalculationParameters
import nl.rijksoverheid.en.applifecycle.AppLifecycleManager
import nl.rijksoverheid.en.config.AppConfigManager
import nl.rijksoverheid.en.job.BackgroundWorkScheduler
import nl.rijksoverheid.en.status.StatusCache
import nl.rijksoverheid.en.test.FakeExposureNotificationApi
import nl.rijksoverheid.en.test.withFragment
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

@Suppress("UNCHECKED_CAST")
@RunWith(AndroidJUnit4::class)
class EnableApiFragmentTest : BaseInstrumentationTest() {

    companion object {
        @ClassRule
        @JvmField
        val disableAnimationsRule: DisableAnimationsRule = DisableAnimationsRule()
    }

    @Before
    fun setup() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val notificationsPreferences = context
        .getSharedPreferences("${BuildConfig.APPLICATION_ID}.notifications", 0)
    private val configPreferences = context
        .getSharedPreferences("${BuildConfig.APPLICATION_ID}.config", 0)

    private val service = object : CdnService {
        override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
            throw NotImplementedError()
        }

        override suspend fun getManifest(cacheHeader: String?): Manifest =
            Manifest(emptyList(), "", "appConfig")

        override suspend fun getRiskCalculationParameters(id: String): RiskCalculationParameters {
            throw NotImplementedError()
        }

        override suspend fun getAppConfig(id: String, cacheHeader: String?) =
            AppConfig(1, 10, 0.0)
    }

    private val repository = ExposureNotificationsRepository(
        context,
        FakeExposureNotificationApi(),
        service,
        notificationsPreferences,
        object : BackgroundWorkScheduler {
            override fun schedule(intervalMinutes: Int) {
            }

            override fun cancel() {
            }
        },
        AppLifecycleManager(configPreferences, AppUpdateManagerFactory.create(context)) {},
        StatusCache(notificationsPreferences),
        AppConfigManager(service)
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

            intended(hasAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS))
            intended(hasData(Uri.parse("package:${BuildConfig.APPLICATION_ID}")))
        }
    }

    @Test
    fun testSkip() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = TestNavHostController(context).apply {
            setViewModelStore(ViewModelStore())
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
                "Skip button opens skip dialog",
                R.id.nav_skip_consent_confirmation, navController.currentDestination?.id
            )
        }
    }
}
