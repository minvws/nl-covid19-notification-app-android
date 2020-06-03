/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.onboarding

import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import nl.rijksoverheid.en.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExplanationFragmentTest {

    @Test
    fun testExplanationStep1() {
        // GIVEN
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.setGraph(R.navigation.nav_onboarding)
        launchFragmentInContainer<ExplanationFragment>(Bundle())
            .onFragment { Navigation.setViewNavController(it.requireView(), navController) }
        navController.setCurrentDestination(R.id.explanationStep1)

        // WHEN
        onView(withId(R.id.next)).perform(click())

        // THEN
        assertEquals(
            "Pressing next button in first step of explanation navigates to second step",
            R.id.explanationStep2, navController.currentDestination?.id
        )
    }

    @Test
    fun testExplanationStep2() {
        // GIVEN
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.setGraph(R.navigation.nav_onboarding)
        launchFragmentInContainer<ExplanationFragment>(Bundle())
            .onFragment { Navigation.setViewNavController(it.requireView(), navController) }
        navController.setCurrentDestination(R.id.explanationStep2)

        // WHEN
        onView(withId(R.id.next)).perform(click())

        // THEN
        assertEquals(
            "Pressing next button in second step of explanation navigates to third step",
            R.id.explanationStep3, navController.currentDestination?.id
        )
    }

    @Test
    fun testExplanationStep3() {
        // GIVEN
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.setGraph(R.navigation.nav_onboarding)
        launchFragmentInContainer<ExplanationFragment>(Bundle())
            .onFragment { Navigation.setViewNavController(it.requireView(), navController) }
        navController.setCurrentDestination(R.id.explanationStep3)

        // WHEN
        onView(withId(R.id.next)).perform(click())

        // THEN
        assertEquals(
            "Pressing next button in third step of explanation navigates to consent screen",
            R.id.nav_enable_api, navController.currentDestination?.id
        )
    }
}
