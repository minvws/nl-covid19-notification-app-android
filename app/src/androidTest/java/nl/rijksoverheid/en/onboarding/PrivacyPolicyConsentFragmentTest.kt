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
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import nl.rijksoverheid.en.BaseInstrumentationTest
import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.CacheStrategy
import nl.rijksoverheid.en.api.CdnService
import nl.rijksoverheid.en.api.model.AppConfig
import nl.rijksoverheid.en.api.model.Manifest
import nl.rijksoverheid.en.applifecycle.AppLifecycleManager
import nl.rijksoverheid.en.config.AppConfigManager
import nl.rijksoverheid.en.job.BackgroundWorkScheduler
import nl.rijksoverheid.en.preferences.AsyncSharedPreferences
import nl.rijksoverheid.en.status.StatusCache
import nl.rijksoverheid.en.test.FakeExposureNotificationApi
import nl.rijksoverheid.en.test.withFragment
import okhttp3.ResponseBody
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

@Suppress("UNCHECKED_CAST")
@RunWith(AndroidJUnit4::class)
class PrivacyPolicyConsentFragmentTest : BaseInstrumentationTest() {

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
    private val onboardingPreferences = context
        .getSharedPreferences("${BuildConfig.APPLICATION_ID}.onboarding", 0)

    private val service = object : CdnService {
        override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> =
            throw NotImplementedError()

        override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest =
            Manifest(emptyList(), "", "appConfig")

        override suspend fun getRiskCalculationParameters(id: String, cacheStrategy: CacheStrategy?) =
            throw NotImplementedError()

        override suspend fun getAppConfig(id: String, cacheStrategy: CacheStrategy?) =
            AppConfig(1, 10, 0.0)

        override suspend fun getResourceBundle(id: String, cacheStrategy: CacheStrategy?) =
            throw NotImplementedError()
    }

    private val repository = ExposureNotificationsRepository(
        context,
        object : FakeExposureNotificationApi() {
            // prevent precondition failures when location is disabled on an emulator
            override fun deviceSupportsLocationlessScanning(): Boolean = true
        },
        service,
        AsyncSharedPreferences { notificationsPreferences },
        object : BackgroundWorkScheduler {
            override fun schedule(intervalMinutes: Int) {
            }

            override fun cancel() {
            }
        },
        AppLifecycleManager(context, configPreferences, AppUpdateManagerFactory.create(context)) {},
        StatusCache(notificationsPreferences),
        AppConfigManager(service)
    )

    private val onboardingRepository = OnboardingRepository(onboardingPreferences) {
        true
    }

    private val onboardingViewModel = OnboardingViewModel(onboardingRepository, repository)
    private val activityViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return when (modelClass) {
                OnboardingViewModel::class.java -> onboardingViewModel as T
                else -> throw IllegalArgumentException("Invalid modelClass")
            }
        }
    }

    @Test
    fun testPrivacyPolicyConsentClickingContainer() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = TestNavHostController(context).apply {
            setGraph(R.navigation.nav_onboarding)
            setCurrentDestination(R.id.nav_privacy_policy_consent)
        }
        withFragment(
            PrivacyPolicyConsentFragment(),
            navController,
            R.style.AppTheme,
            activityViewModelFactory
        ) {
            onView(withId(R.id.consent_checkbox)).check(matches(isNotChecked()))
            onView(withId(R.id.next)).check(matches(not(isEnabled())))

            onView(withId(R.id.consent_container)).perform(click())

            onView(withId(R.id.consent_checkbox)).check(matches(isChecked()))
            onView(withId(R.id.next)).check(matches(isEnabled()))

            onView(withId(R.id.next)).perform(click())

            assertEquals(
                "Pressing next button in privacy policy consent screen navigates to enable api screen",
                R.id.nav_enable_api, navController.currentDestination?.id
            )
        }
    }

    @Test
    fun testPrivacyPolicyConsentClickingCheckbox() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = TestNavHostController(context).apply {
            setGraph(R.navigation.nav_onboarding)
            setCurrentDestination(R.id.nav_privacy_policy_consent)
        }
        withFragment(
            PrivacyPolicyConsentFragment(),
            navController,
            R.style.AppTheme,
            activityViewModelFactory
        ) {
            onView(withId(R.id.consent_checkbox)).check(matches(isNotChecked()))
            onView(withId(R.id.next)).check(matches(not(isEnabled())))

            onView(withId(R.id.consent_checkbox)).perform(click())

            onView(withId(R.id.consent_checkbox)).check(matches(isChecked()))
            onView(withId(R.id.next)).check(matches(isEnabled()))
        }
    }
}
