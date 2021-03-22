/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import android.content.Context
import androidx.core.os.ConfigurationCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocaleHelperTest {

    @Before
    fun `setup and test that LocaleHelper getInstance throws exception before initialization`() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        if (isInitialized) {
            LocaleHelper.getInstance().useAppInDutch(false, appContext)
        } else {
            Assert.assertThrows(IllegalStateException::class.java) { LocaleHelper.getInstance() }
            LocaleHelper.initialize(ApplicationProvider.getApplicationContext())
            isInitialized = true
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `LocaleHelper throws exception on initialization when already initialized`() {
        LocaleHelper.initialize(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `LocaleHelper isAppSetToDutch is false by default`() {
        val localeHelper = LocaleHelper.getInstance()
        assertFalse(localeHelper.isAppSetToDutch)
    }

    @Test
    fun `LocaleHelper isSystemLanguageDutch is false by default`() {
        val localeHelper = LocaleHelper.getInstance()
        assertFalse(localeHelper.isSystemLanguageDutch)
    }

    @Test
    fun `LocaleHelper useAppInDutch(true) sets provided context locale but not system locale to Dutch`() {
        val localeHelper = LocaleHelper.getInstance()
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        localeHelper.useAppInDutch(true, appContext)
        assert(ConfigurationCompat.getLocales(appContext.resources.configuration)[0] == LocaleHelper.dutchLocale)
        assert(localeHelper.isAppSetToDutch)
        assert(!localeHelper.isSystemLanguageDutch)
    }

    @Test
    fun `LocaleHelper useAppInDutch(false) sets provided context locale to system locale`() {
        val localeHelper = LocaleHelper.getInstance()
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val initialLocale = ConfigurationCompat.getLocales(appContext.resources.configuration)[0]
        localeHelper.useAppInDutch(false, appContext)
        assert(ConfigurationCompat.getLocales(appContext.resources.configuration)[0] == initialLocale)
        assert(localeHelper.isAppSetToDutch == localeHelper.isSystemLanguageDutch)
    }

    companion object {
        private var isInitialized = false
    }
}
