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
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.rijksoverheid.en.BaseInstrumentationTest
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.test.withFragment
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("UNCHECKED_CAST")
@RunWith(AndroidJUnit4::class)
class PrivacyPolicyConsentFragmentTest : BaseInstrumentationTest() {

    @Test
    fun testPrivacyPolicyConsentClickingContainer() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = TestNavHostController(context).apply {
            setGraph(R.navigation.nav_onboarding)
            setCurrentDestination(R.id.nav_privacy_policy_consent)
        }
        withFragment(
            PrivacyPolicyConsentFragment(),
            navController,
            R.style.AppTheme
        ) {
            onView(withId(R.id.consent_checkbox)).check(matches(isNotChecked()))
            onView(withId(R.id.next)).check(matches(not(isEnabled())))

            onView(withId(R.id.consent_container)).perform(click())

            onView(withId(R.id.consent_checkbox)).check(matches(isChecked()))
            onView(withId(R.id.next)).check(matches(isEnabled()))

            onView(withId(R.id.next)).perform(click())

            assertEquals(
                "Pressing next button in privacy policy consent screen navigates to enable api screen",
                R.id.nav_enable_api, navController.currentDestination?.id
            )
        }
    }

    @Test
    fun testPrivacyPolicyConsentClickingCheckbox() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = TestNavHostController(context).apply {
            setGraph(R.navigation.nav_onboarding)
            setCurrentDestination(R.id.nav_privacy_policy_consent)
        }
        withFragment(
            PrivacyPolicyConsentFragment(),
            navController,
            R.style.AppTheme
        ) {
            onView(withId(R.id.consent_checkbox)).check(matches(isNotChecked()))
            onView(withId(R.id.next)).check(matches(not(isEnabled())))

            onView(withId(R.id.consent_checkbox)).perform(click())

            onView(withId(R.id.consent_checkbox)).check(matches(isChecked()))
            onView(withId(R.id.next)).check(matches(isEnabled()))
        }
    }
}
