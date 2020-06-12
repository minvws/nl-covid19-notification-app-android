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
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bartoszlipinski.disableanimationsrule.DisableAnimationsRule
import nl.rijksoverheid.en.BaseInstrumentationTest
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.test.withFragment
import org.junit.Assert.assertEquals
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExplanationFragmentTest : BaseInstrumentationTest() {

    companion object {
        @ClassRule
        @JvmField
        val disableAnimationsRule: DisableAnimationsRule = DisableAnimationsRule()
    }

    @Test
    fun testExplanationStep1To2() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = TestNavHostController(context).apply {
            setGraph(R.navigation.nav_onboarding)
            setCurrentDestination(R.id.explanationStep1)
        }
        withFragment(ExplanationFragment(), navController, R.style.AppTheme) {
            onView(withId(R.id.headline)).check(matches(withText(R.string.onboarding_explanation_1_headline)))
            onView(withId(R.id.description)).check(matches(withText(R.string.onboarding_explanation_1_description)))

            onView(withId(R.id.next)).perform(click())

            assertEquals(
                "Pressing next button in first step of explanation navigates to second step",
                R.id.explanationStep2, navController.currentDestination?.id
            )
            reportHelper.label("Explanation step 1")
        }
    }

    @Test
    fun testExplanationStep2To3() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = TestNavHostController(context).apply {
            setGraph(R.navigation.nav_onboarding)
            setCurrentDestination(R.id.explanationStep2)
        }

        withFragment(ExplanationFragment(), navController, R.style.AppTheme) {
            onView(withId(R.id.headline)).check(matches(withText(R.string.onboarding_explanation_2_headline)))
            onView(withId(R.id.description)).check(matches(withText(R.string.onboarding_explanation_2_description)))

            onView(withId(R.id.next)).perform(click())

            assertEquals(
                "Pressing next button in second step of explanation navigates to third step",
                R.id.explanationStep3, navController.currentDestination?.id
            )

            reportHelper.label("Explanation step 2")
        }
    }

    @Test
    fun testExplanationStep3ToConsent() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = TestNavHostController(context).apply {
            setGraph(R.navigation.nav_onboarding)
            setCurrentDestination(R.id.explanationStep3)
        }

        withFragment(ExplanationFragment(), navController, R.style.AppTheme) {
            onView(withId(R.id.headline)).check(matches(withText(R.string.onboarding_explanation_3_headline)))
            onView(withId(R.id.description)).check(matches(withText(R.string.onboarding_explanation_3_description)))

            onView(withId(R.id.next)).perform(click())

            assertEquals(
                "Pressing next button in third step of explanation navigates to consent screen",
                R.id.nav_enable_api, navController.currentDestination?.id
            )

            reportHelper.label("Explanation step 3")
        }
    }
}
