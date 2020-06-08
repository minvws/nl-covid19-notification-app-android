/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.onboarding

import android.content.Context
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.rijksoverheid.en.BaseInstrumentationTest
import nl.rijksoverheid.en.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExplanationFragmentTest : BaseInstrumentationTest() {

    @Test
    fun testExplanationStep1To2() {
        // GIVEN
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = TestNavHostController(context).apply {
            setGraph(R.navigation.nav_onboarding)
            setCurrentDestination(R.id.explanationStep1)
        }
        val args = navController.currentBackStackEntry?.arguments
        launchFragmentInContainer<ExplanationFragment>(args, R.style.AppTheme)
            .onFragment { Navigation.setViewNavController(it.requireView(), navController) }

        // WHEN
        onView(withId(R.id.next)).perform(click())

        // THEN
        assertEquals(
            "Pressing next button in first step of explanation navigates to second step",
            R.id.explanationStep2, navController.currentDestination?.id
        )
        onView(withId(R.id.headline)).check(matches(withText(R.string.onboarding_explanation_1_headline)))
        onView(withId(R.id.description)).check(matches(withText(R.string.onboarding_explanation_1_description)))
        onView(withId(R.id.illustration)).check(matches(withContentDescription(R.string.cd_illustration_explanation_step_1)))
    }

    @Test
    fun testExplanationStep2To3() {
        // GIVEN
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = TestNavHostController(context).apply {
            setGraph(R.navigation.nav_onboarding)
            setCurrentDestination(R.id.explanationStep2)
        }
        val args = navController.currentBackStackEntry?.arguments
        launchFragmentInContainer<ExplanationFragment>(args, R.style.AppTheme)
            .onFragment { Navigation.setViewNavController(it.requireView(), navController) }

        // WHEN
        onView(withId(R.id.next)).perform(click())

        // THEN
        assertEquals(
            "Pressing next button in second step of explanation navigates to third step",
            R.id.explanationStep3, navController.currentDestination?.id
        )
        onView(withId(R.id.headline)).check(matches(withText(R.string.onboarding_explanation_2_headline)))
        onView(withId(R.id.description)).check(matches(withText(R.string.onboarding_explanation_2_description)))
        onView(withId(R.id.illustration)).check(matches(withContentDescription(R.string.cd_illustration_explanation_step_2)))
    }

    @Test
    fun testExplanationStep3ToConsent() {
        // GIVEN
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = TestNavHostController(context).apply {
            setGraph(R.navigation.nav_onboarding)
            setCurrentDestination(R.id.explanationStep3)
        }
        val args = navController.currentBackStackEntry?.arguments
        launchFragmentInContainer<ExplanationFragment>(args, R.style.AppTheme)
            .onFragment { Navigation.setViewNavController(it.requireView(), navController) }

        // WHEN
        onView(withId(R.id.next)).perform(click())

        // THEN
        assertEquals(
            "Pressing next button in third step of explanation navigates to consent screen",
            R.id.nav_enable_api, navController.currentDestination?.id
        )
        onView(withId(R.id.headline)).check(matches(withText(R.string.onboarding_explanation_3_headline)))
        onView(withId(R.id.description)).check(matches(withText(R.string.onboarding_explanation_3_description)))
        onView(withId(R.id.illustration)).check(matches(withContentDescription(R.string.cd_illustration_explanation_step_3)))
    }
}
