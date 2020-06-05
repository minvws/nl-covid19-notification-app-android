/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import com.microsoft.appcenter.espresso.Factory
import com.microsoft.appcenter.espresso.ReportHelper
import org.junit.After
import org.junit.Rule

abstract class BaseInstrumentationTest {
    @JvmField
    @Rule
    val reportHelper: ReportHelper = Factory.getReportHelper()

    @After
    open fun tearDown() {
        reportHelper.label("End of test")
    }
}
