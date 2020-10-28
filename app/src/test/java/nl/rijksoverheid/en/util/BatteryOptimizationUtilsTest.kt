/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import nl.rijksoverheid.en.BuildConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class BatteryOptimizationUtilsTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `isIgnoringBatteryOptimizations returns true if battery optimisations are ignored`() {
        val powerManager = shadowOf(context.getSystemService(PowerManager::class.java))
        val packageManager = shadowOf(context.packageManager)
        packageManager.addActivityIfNotPresent(ComponentName("com.test", "com.test.TestActivity"))

        packageManager.addIntentFilterForActivity(
            ComponentName("com.test", "com.test.TestActivity"),
            IntentFilter(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            ).apply {
                addDataScheme("package")
                addCategory(Intent.CATEGORY_DEFAULT)
            }
        )
        powerManager.setIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID, true)
        assertTrue(context.isIgnoringBatteryOptimizations())
    }

    @Test
    fun `isIgnoringBatteryOptimizations returns false if battery optimisations are not ignored`() {
        val powerManager = shadowOf(context.getSystemService(PowerManager::class.java))
        val packageManager = shadowOf(context.packageManager)
        packageManager.addActivityIfNotPresent(ComponentName("com.test", "com.test.TestActivity"))

        packageManager.addIntentFilterForActivity(
            ComponentName("com.test", "com.test.TestActivity"),
            IntentFilter(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            ).apply {
                addDataScheme("package")
                addCategory(Intent.CATEGORY_DEFAULT)
            }
        )
        powerManager.setIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID, false)
        assertFalse(context.isIgnoringBatteryOptimizations())
    }

    @Test
    fun `isIgnoringBatteryOptimizations returns true if battery no activity exists for disabling the setting`() {
        val powerManager = shadowOf(context.getSystemService(PowerManager::class.java))
        powerManager.setIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID, false)
        assertTrue(context.isIgnoringBatteryOptimizations())
    }
}
