/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ApplicationProvider
import nl.rijksoverheid.en.settings.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

private val DUTCH_LOCALE = Locale("nl", "NL")

@RunWith(RobolectricTestRunner::class)
class LocaleHelperTest {

    private lateinit var applicationContext: Context

    @Before
    fun setup() {
        applicationContext = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `LocaleHelper isAppSetToDutch is false by default`() {
        val localeHelper =
            LocaleHelper(applicationContext) { /* dummy */ }
        assertFalse(localeHelper.isAppSetToDutch)
    }

    @Test
    fun `LocaleHelper applies locale`() {
        val settings = Settings(ApplicationProvider.getApplicationContext())
        val appliedLocales = AtomicReference(LocaleListCompat.getEmptyLocaleList())
        val localeHelper = LocaleHelper(applicationContext, settings) { appliedLocales.set(it) }
        val appContext = ApplicationProvider.getApplicationContext<Application>()

        localeHelper.useAppInDutch(true)
        assertEquals(LocaleListCompat.create(DUTCH_LOCALE), appliedLocales.get())
        assertTrue(
            ConfigurationCompat.getLocales(localeHelper.createContextForLocale(appContext).resources.configuration)[0] == Locale(
                "nl",
                "NL"
            )
        )
        assertTrue(localeHelper.isAppSetToDutch)
        // setting should be updated
        assertTrue(settings.isAppSetToDutch)
    }

    @Test
    fun `LocaleHelper restores default locale`() {
        val settings = Settings(ApplicationProvider.getApplicationContext())
        settings.isAppSetToDutch = true
        val appliedLocales = AtomicReference(LocaleListCompat.create(DUTCH_LOCALE))
        val localeHelper = LocaleHelper(applicationContext, settings) { appliedLocales.set(it) }

        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val defaultLocale = ConfigurationCompat.getLocales(appContext.resources.configuration)

        localeHelper.useAppInDutch(false)
        assertEquals(LocaleListCompat.getEmptyLocaleList(), appliedLocales.get())

        assertTrue(ConfigurationCompat.getLocales(localeHelper.createContextForLocale(appContext).resources.configuration) == defaultLocale)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `LocaleHelper wraps locale when API level is lower than 33`() {
        val settings = Settings(ApplicationProvider.getApplicationContext())
        val localeHelper = LocaleHelper(applicationContext, settings) { /* nothing */ }
        settings.isAppSetToDutch = true
        val appContext = ApplicationProvider.getApplicationContext<Application>()
        assertTrue(
            ConfigurationCompat.getLocales(localeHelper.createContextForLocale(appContext).resources.configuration)[0] == Locale(
                "nl",
                "NL"
            )
        )
    }

    @Test
    @Ignore("Tiramisu is not yet supported by Robolectric")
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `LocaleHelper does not wrap locale when API level 33 or up`() {
        val settings = Settings(ApplicationProvider.getApplicationContext())
        val localeHelper = LocaleHelper(applicationContext, settings) { /* nothing */ }
        settings.isAppSetToDutch = true
        val appContext = ApplicationProvider.getApplicationContext<Application>()
        val expectedLocales = appContext.resources.configuration.locales
        assertTrue(
            ConfigurationCompat.getLocales(localeHelper.createContextForLocale(appContext).resources.configuration) == LocaleListCompat.wrap(
                expectedLocales
            )
        )
    }
}
