/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.requesttest

import android.content.Context
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.rijksoverheid.en.BaseInstrumentationTest
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.status.StatusFragmentDirections
import nl.rijksoverheid.en.test.withFragment
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RequestTestFragmentTest : BaseInstrumentationTest() {

    @Test
    fun testRequestTestShowsPhoneNumber() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val navController = TestNavHostController(context).apply {
            setGraph(R.navigation.nav_main)
            setCurrentDestination(R.id.nav_status)
        }

        navController.navigate(StatusFragmentDirections.actionRequestTest("12345"))

        val fragment = RequestTestFragment()

        withFragment(fragment, navController, R.style.Theme_CoronaMelder) {
            val expectedLabel = context.getString(
                R.string.request_test_button_call,
                "12345"
            )
            Espresso.onView(ViewMatchers.withId(R.id.button_1))
                .check(ViewAssertions.matches(ViewMatchers.withText(expectedLabel)))
        }
    }
}
