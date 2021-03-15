/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import nl.rijksoverheid.en.settings.Settings
import java.util.Locale

class LocaleHelper private constructor(application: Application, private val settings: Settings) {

    // Ensure systemLocale is set and ConfigurationChangedCallback is registered _before_
    // custom Locale is applied, as the system locale can not be reliably retrieved after
    // changing the app's Locale.
    private var systemLocale: Locale = LocaleListCompat.getDefault()[0]

    val isAppSetToDutch: Boolean
        get() = settings.isAppSetToDutch

    val isSystemLanguageDutch
        get() = systemLocale == dutchLocale

    private val localeToUse: Locale
        get() = if (isAppSetToDutch) dutchLocale else systemLocale

    init {
        application.registerActivityLifecycleCallbacks(
            ActivityCreatedCallback { activity ->
                applyLocale(activity, localeToUse)
            }
        )

        application.registerComponentCallbacks(
            ConfigurationChangedCallback { configuration ->
                systemLocale = ConfigurationCompat.getLocales(configuration)[0]
                applyLocale(application, localeToUse)
            }
        )
        applyLocale(application, localeToUse)
    }

    fun useAppInDutch(useAppInDutch: Boolean, context: Context): Boolean {
        return if (useAppInDutch != isAppSetToDutch) {
            settings.isAppSetToDutch = useAppInDutch
            applyLocale(context, localeToUse)
            true
        } else {
            false
        }
    }

    private fun applyLocale(context: Context, locale: Locale) {
        updateResources(context.resources, locale)
        val appContext = context.applicationContext
        if (appContext !== context) {
            updateResources(appContext.resources, locale)
        }
    }

    @Suppress("DEPRECATION")
    private fun updateResources(resources: Resources, locale: Locale) {
        val currentLocale = ConfigurationCompat.getLocales(resources.configuration)[0]
        if (currentLocale == locale) return
        Locale.setDefault(locale)
        resources.apply {
            val config = Configuration(configuration).apply {
                setLocale(locale)
            }
            updateConfiguration(config, displayMetrics)
        }
    }

    companion object {

        val dutchLocale: Locale = Locale("nl", "NL")

        private lateinit var localeHelper: LocaleHelper

        fun initialize(application: Application) {
            check(!::localeHelper.isInitialized) { "LocaleHelper was already initialized." }
            localeHelper = LocaleHelper(application, Settings(application))
        }

        fun getInstance(): LocaleHelper {
            check(::localeHelper.isInitialized) { "LocaleHelper was not initialized. Ensure it is initialized in Application.onCreate." }
            return localeHelper
        }
    }
}
