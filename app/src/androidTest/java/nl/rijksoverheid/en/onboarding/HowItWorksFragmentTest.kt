/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.onboarding

import android.content.Context
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bartoszlipinski.disableanimationsrule.DisableAnimationsRule
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.test.withFragment
import org.junit.Assert
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("UNCHECKED_CAST")
@RunWith(AndroidJUnit4::class)
class HowItWorksFragmentTest {

    companion object {
        @ClassRule
        @JvmField
        val disableAnimationsRule: DisableAnimationsRule = DisableAnimationsRule()
    }

    @Test
    fun testGotoDetail() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = TestNavHostController(context).apply {
            setGraph(R.navigation.nav_onboarding)
            setCurrentDestination(R.id.nav_how_it_works)
        }
        withFragment(HowItWorksFragment(), navController, R.style.AppTheme) {
            Espresso.onView(withText(R.string.faq_location)).perform(click())

            Assert.assertEquals(
                "Pressing FAQ item navigates to detail page",
                R.id.nav_how_it_works_detail, navController.currentDestination?.id
            )
        }
    }

    @Test
    fun testGoBack() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = TestNavHostController(context).apply {
            setGraph(R.navigation.nav_onboarding)
            setCurrentDestination(R.id.nav_how_it_works)
        }
        withFragment(HowItWorksFragment(), navController, R.style.AppTheme) {
            Espresso.onView(withContentDescription(R.string.cd_close)).perform(click())

            Assert.assertEquals(
                "Pressing toolbar close icon navigates back",
                R.id.explanationStep1, navController.currentDestination?.id
            )
        }
    }
}
