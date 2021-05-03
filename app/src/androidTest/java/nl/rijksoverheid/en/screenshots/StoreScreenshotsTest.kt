/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.screenshots

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.runner.screenshot.ScreenCapture
import androidx.test.runner.screenshot.ScreenCaptureProcessor
import androidx.test.runner.screenshot.Screenshot
import androidx.test.runner.screenshot.Screenshot.capture
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.vanniktech.junit4androidintegrationrules.BatteryCommand.Companion.battery
import com.vanniktech.junit4androidintegrationrules.ClockCommand
import com.vanniktech.junit4androidintegrationrules.DemoModeRule
import com.vanniktech.junit4androidintegrationrules.NetworkCommand
import com.vanniktech.junit4androidintegrationrules.NetworkCommand.Companion.network
import com.vanniktech.junit4androidintegrationrules.NotificationsCommand.Companion.notifications
import com.vanniktech.junit4androidintegrationrules.StatusCommand
import com.vanniktech.junit4androidintegrationrules.StatusCommand.Companion.status
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
import nl.rijksoverheid.en.labtest.LabTestFragment
import nl.rijksoverheid.en.notifier.NotificationsRepository
import nl.rijksoverheid.en.onboarding.OnboardingRepository
import nl.rijksoverheid.en.preferences.AsyncSharedPreferences
import nl.rijksoverheid.en.requesttest.RequestTestFragment
import nl.rijksoverheid.en.requesttest.RequestTestFragmentArgs
import nl.rijksoverheid.en.settings.Settings
import nl.rijksoverheid.en.settings.SettingsRepository
import nl.rijksoverheid.en.status.StatusCache
import nl.rijksoverheid.en.status.StatusFragment
import nl.rijksoverheid.en.status.StatusViewModel
import nl.rijksoverheid.en.test.FakeExposureNotificationApi
import nl.rijksoverheid.en.test.withFragment
import okhttp3.ResponseBody
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import tools.fastlane.screengrab.locale.LocaleTestRule
import java.io.File
import java.io.FileOutputStream
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * UI test to create store listing screenshots for various locales
 */
@Suppress("UNCHECKED_CAST")
@StoreScreenshots
class StoreScreenshotsTest : BaseInstrumentationTest() {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val notificationsPreferences = context
        .getSharedPreferences("${BuildConfig.APPLICATION_ID}.notifications", 0)
    private val configPreferences = context
        .getSharedPreferences("${BuildConfig.APPLICATION_ID}.config", 0)
    private val settingsPreferences = context
        .getSharedPreferences("${BuildConfig.APPLICATION_ID}.settings", 0)

    @Rule @JvmField
    val localeTestRule = LocaleTestRule()

    @Rule
    @JvmField
    val demoModeRule: DemoModeRule = DemoModeRule(
        notifications().visible(false),
        network()
            .wifi(false)
            .mobileDataType(NetworkCommand.MobileDataType.MOBILE_DATA_TYPE_4G)
            .mobileLevel(NetworkCommand.MobileLevel.MOBILE_LEVEL_4)
            .nosim(false)
            .mobile(true),
        battery().level(80).plugged(false).powersave(false),
        status().bluetooth(StatusCommand.BluetoothMode.BLUETOOTH_MODE_HIDDEN)
            .mute(false)
            .volume(StatusCommand.VolumeMode.VOLUME_MODE_SILENT),
        ClockCommand.clock().hhmm("1200")
    )

    init {
        Screenshot.setScreenshotProcessors(setOf(StoreScreenshotProcessor(context)))
    }

    private val clock = Clock.fixed(Instant.parse("2020-06-03T10:15:30.00Z"), ZoneId.of("UTC"))

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

    private val repository = ExposureNotificationsRepository(
        context,
        object : FakeExposureNotificationApi() {
            override suspend fun getStatus(): StatusResult = StatusResult.Enabled
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
        AppConfigManager(service),
        clock = clock
    )
    private val settingsRepository = SettingsRepository(
        context, Settings(context, settingsPreferences)
    )

    private val statusViewModel = StatusViewModel(
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
    private val viewModel = ExposureNotificationsViewModel(repository, settingsRepository)
    private val activityViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return viewModel as T
        }
    }

    @Test
    fun takeStatusGreenScreenshot() {
        val navController = TestNavHostController(context).apply {
            setGraph(R.navigation.nav_main)
            setCurrentDestination(R.id.nav_status)
        }

        notificationsPreferences.edit {
            clear()
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
            R.style.AppTheme,
            activityViewModelFactory
        ) {
            onView(withId(R.id.status_animated_image))
            Thread.sleep(5000)
            capture().setName("status_screen").process()
        }
    }

    @Test
    fun takeStatusRedScreenshot() {
        val navController = TestNavHostController(context).apply {
            setGraph(R.navigation.nav_main)
            setCurrentDestination(R.id.nav_status)
        }

        notificationsPreferences.edit(commit = true) {
            putLong("last_notification_received_date", LocalDate.now(clock).toEpochDay())
            putLong("last_token_exposure_date", LocalDate.now(clock).minusDays(2).toEpochDay())
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
            R.style.AppTheme,
            activityViewModelFactory
        ) {
            onView(withId(R.id.status_animated_image))
            Thread.sleep(5000)
            capture().setName("status_screen_red").process()
        }
    }

    @Test
    fun takeLabTestScreenshot() {
        val navController = TestNavHostController(context).apply {
            setGraph(R.navigation.nav_main)
            setCurrentDestination(R.id.nav_lab_test)
        }

        withFragment(
            LabTestFragment(),
            navController,
            R.style.AppTheme,
            activityViewModelFactory
        ) {
            onView(withId(R.id.content))
            Thread.sleep(5000)
            capture().setName("lab_test").process()
        }
    }

    @Test
    fun takeRequestScreenshot() {
        val navController = TestNavHostController(context).apply {
            setGraph(R.navigation.nav_main)
            setCurrentDestination(
                R.id.requestTestFragment,
                RequestTestFragmentArgs(context.getString(R.string.request_test_phone_number)).toBundle()
            )
        }

        withFragment(
            RequestTestFragment(),
            navController,
            R.style.AppTheme,
            activityViewModelFactory
        ) {
            onView(withId(R.id.content))
            Thread.sleep(3000)
            capture().setName("request_test").process()
        }
    }
}

private class StoreScreenshotProcessor(private val context: Context) : ScreenCaptureProcessor {
    override fun process(capture: ScreenCapture): String {
        val dest = File(
            context.getExternalFilesDir(null),
            "screenshots/${Locale.getDefault()}/${capture.name}.png"
        )
        dest.parentFile!!.mkdirs()
        FileOutputStream(dest).use {
            capture.bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return dest.name
    }
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class StoreScreenshots
