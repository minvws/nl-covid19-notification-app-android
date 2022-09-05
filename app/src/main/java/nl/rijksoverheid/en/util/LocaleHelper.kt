/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat
import nl.rijksoverheid.en.settings.Settings
import java.util.Locale

private val DUTCH_LOCALE = Locale("nl", "NL")

/**
 * Helper class for applying the correct language based on the system or in app setting.
 */
class LocaleHelper constructor(
    private val context: Context,
    private val settings: Settings = Settings(context),
    /* for testing */
    private val setApplicationLocales: (LocaleListCompat) -> Unit = { AppCompatDelegate.setApplicationLocales(it) }
) {
    private val systemLocale: Locale = LocaleManagerCompat.getSystemLocales(context)[0] ?: Locale.getDefault()

    val isAppSetToDutch: Boolean
        get() = settings.isAppSetToDutch

    val isSystemLanguageDutch
        get() = systemLocale.language == DUTCH_LOCALE.language

    fun useAppInDutch(useAppInDutch: Boolean) {
        settings.isAppSetToDutch = useAppInDutch
        applyLocale()
    }

    fun applyLocale() {
        if (isAppSetToDutch) {
            setApplicationLocales(LocaleListCompat.create(DUTCH_LOCALE))
        } else {
            setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        }
    }

    /**
     * Wraps a context with the appropriate locale, based on [isAppSetToDutch]
     * This is only required for notifications where AppCompateDelegate does not affect
     * the context. On Android 13 and up this method is a no-op as locale changes
     * are handled by the system in that case.
     *
     * @param context the context to set the locale configuration on
     * @return a context with the configured locale
     */
    fun createContextForLocale(context: Context): Context {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context
        } else {
            context.createConfigurationContext(updateConfiguration(context.resources.configuration))
        }
    }

    private fun updateConfiguration(configuration: Configuration): Configuration {
        val updatedConfig = Configuration(configuration)
        if (isAppSetToDutch) {
            updatedConfig.setLocale(DUTCH_LOCALE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                updatedConfig.setLocales(LocaleList(DUTCH_LOCALE))
            }
        }
        return updatedConfig
    }
}

/**
 * Applies the app locale using [LocaleHelper.createContextForLocale]
 */
fun Context.withAppLocale(): Context = LocaleHelper(this).createContextForLocale(this)
