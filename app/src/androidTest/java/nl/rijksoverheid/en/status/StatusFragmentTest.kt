/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.en.BaseInstrumentationTest
import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.ExposureNotificationsViewModel
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.CacheStrategy
import nl.rijksoverheid.en.api.CdnService
import nl.rijksoverheid.en.api.model.AppConfig
import nl.rijksoverheid.en.api.model.Manifest
import nl.rijksoverheid.en.api.model.ResourceBundle
import nl.rijksoverheid.en.api.model.RiskCalculationParameters
import nl.rijksoverheid.en.applifecycle.AppLifecycleManager
import nl.rijksoverheid.en.config.AppConfigManager
import nl.rijksoverheid.en.enapi.StatusResult
import nl.rijksoverheid.en.factory.createAppConfigManager
import nl.rijksoverheid.en.job.BackgroundWorkScheduler
import nl.rijksoverheid.en.notifier.NotificationsRepository
import nl.rijksoverheid.en.onboarding.OnboardingRepository
import nl.rijksoverheid.en.preferences.AsyncSharedPreferences
import nl.rijksoverheid.en.settings.Settings
import nl.rijksoverheid.en.settings.SettingsRepository
import nl.rijksoverheid.en.test.FakeExposureNotificationApi
import nl.rijksoverheid.en.test.withFragment
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit

@Suppress("UNCHECKED_CAST")
@RunWith(AndroidJUnit4::class)
class StatusFragmentTest : BaseInstrumentationTest() {

    private var preferencesFactory: suspend () -> SharedPreferences = { notificationsPreferences }

    private val clock = object : Clock() {
        var instant = Instant.parse("2020-06-20T10:15:30.00Z")
        override fun withZone(zone: ZoneId?): Clock = this
        override fun getZone() = ZoneId.of("UTC")
        override fun instant() = instant

        fun passTime(amountToAdd: Long, unit: TemporalUnit) {
            this.instant = instant.plus(amountToAdd, unit)
        }
    }
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val notificationsPreferences = context
        .getSharedPreferences("${BuildConfig.APPLICATION_ID}.notifications", 0)
    private val configPreferences = context
        .getSharedPreferences("${BuildConfig.APPLICATION_ID}.config", 0)
    private val settingsPreferences = context
        .getSharedPreferences("${BuildConfig.APPLICATION_ID}.settings", 0)

    private val service = object : CdnService {
        override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
            throw NotImplementedError()
        }

        override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest =
            Manifest(emptyList(), "", "appConfig")

        override suspend fun getRiskCalculationParameters(
            id: String,
            cacheStrategy: CacheStrategy?
        ): RiskCalculationParameters {
            throw NotImplementedError()
        }

        override suspend fun getAppConfig(id: String, cacheStrategy: CacheStrategy?) =
            AppConfig(1, 10, 0.0)

        override suspend fun getResourceBundle(
            id: String,
            cacheStrategy: CacheStrategy?
        ): ResourceBundle {
            throw IllegalStateException()
        }
    }

    private val repository by lazy {
        ExposureNotificationsRepository(
            context,
            object : FakeExposureNotificationApi() {
                override suspend fun getStatus(): StatusResult = StatusResult.Enabled
                override fun deviceSupportsLocationlessScanning(): Boolean = true
            },
            service,
            AsyncSharedPreferences { preferencesFactory() },
            object : BackgroundWorkScheduler {
                override fun schedule(intervalMinutes: Int) {
                }

                override fun cancel() {
                }
            },
            AppLifecycleManager(
                context,
                configPreferences,
                AppUpdateManagerFactory.create(context)
            ) {},
            StatusCache(notificationsPreferences),
            AppConfigManager(service, { false }, { emptyList() }),
            clock = clock
        )
    }
    private val settingsRepository by lazy {
        SettingsRepository(
            context, Settings(context, settingsPreferences)
        )
    }

    private val statusViewModel by lazy {
        StatusViewModel(
            OnboardingRepository(
                sharedPreferences = configPreferences,
                googlePlayServicesUpToDateChecker = { true }
            ),
            repository,
            NotificationsRepository(context, clock),
            settingsRepository,
            createAppConfigManager(context),
            clock
        )
    }
    private val viewModel by lazy { ExposureNotificationsViewModel(repository, settingsRepository) }
    private val activityViewModelFactory by lazy {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return viewModel as T
            }
        }
    }

    @Before
    fun setup() {
        notificationsPreferences.edit {
            clear()
        }
    }

    @Test
    fun testRemoveConnectionIssuesNotification() {
        runBlocking { repository.requestEnableNotifications() }
        clock.passTime(2, ChronoUnit.DAYS)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = TestNavHostController(context).apply {
            setGraph(R.navigation.nav_main)
            setCurrentDestination(R.id.nav_status)
        }

        withFragment(
            StatusFragment(
                factoryProducer = {
                    object : ViewModelProvider.Factory {
                        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                            return statusViewModel as T
                        }
                    }
                }
            ),
            navController,
            R.style.Theme_CoronaMelder,
            activityViewModelFactory
        ) {
            onView(withId(R.id.status_headline))
                .check(matches(withText(R.string.status_partly_active_headline)))

            onView(withId(R.id.status_description))
                .check(matches(withText(R.string.status_error_sync_issues)))

            onView(withId(R.id.disabled_enable)).check(matches(withText(R.string.status_error_action_sync_issues)))
                .perform(click())

            onView(withId(R.id.status_headline)).check(matches(withText(R.string.status_no_exposure_detected_headline)))
            onView(withId(R.id.status_description)).check(matches(withText(R.string.status_no_exposure_detected_description)))
        }
    }

    @Test
    fun testWaitingForPreferencesLoadedShowsLoadingScreen() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = TestNavHostController(context).apply {
            setGraph(R.navigation.nav_main)
            setCurrentDestination(R.id.nav_status)
        }

        preferencesFactory = {
            delay(10000)
            notificationsPreferences
        }

        withFragment(
            StatusFragment(
                factoryProducer = {
                    object : ViewModelProvider.Factory {
                        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                            return statusViewModel as T
                        }
                    }
                }
            ),
            navController,
            R.style.Theme_CoronaMelder,
            activityViewModelFactory
        ) {
            onView(withId(R.id.loading)).check(matches(isDisplayed()))
        }
    }
}
