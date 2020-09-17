/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.applifecycle

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppLifecycleManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `getUpdateState with available update from Google Play should return InAppUpdate`() =
        runBlocking {
            context.packageManager.setInstallerPackageName(
                "nl.rijksoverheid.en",
                "com.android.vending"
            )
            val appUpdateManager = FakeAppUpdateManager(context)
            val preferences = context.getSharedPreferences("test_update", 0)
            val appLifecycleManager = AppLifecycleManager(context, preferences, appUpdateManager) {
                /* nothing */
            }

            appUpdateManager.setUpdateAvailable(123)
            appLifecycleManager.verifyMinimumVersion(123, false)

            val result = appLifecycleManager.getUpdateState()

            assertTrue(result is AppLifecycleManager.UpdateState.InAppUpdate)
        }

    @Test
    fun `getUpdateState with available update from when not installed from Google Play should return UpdateRequired`() =
        runBlocking {
            context.packageManager.setInstallerPackageName(
                "nl.rijksoverheid.en",
                "com.some.appstore"
            )
            val appUpdateManager = FakeAppUpdateManager(context)
            val preferences = context.getSharedPreferences("test_update", 0)
            val appLifecycleManager = AppLifecycleManager(context, preferences, appUpdateManager) {
                /* nothing */
            }

            appUpdateManager.setUpdateAvailable(123)
            appLifecycleManager.verifyMinimumVersion(123, false)

            val result = appLifecycleManager.getUpdateState()

            assertTrue(result is AppLifecycleManager.UpdateState.UpdateRequired)
            assertEquals(
                "com.some.appstore",
                (result as AppLifecycleManager.UpdateState.UpdateRequired).installerPackageName
            )
        }
}
