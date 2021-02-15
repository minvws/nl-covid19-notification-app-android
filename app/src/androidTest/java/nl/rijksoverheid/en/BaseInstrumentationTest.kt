/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.bartoszlipinski.disableanimationsrule.DisableAnimationsRule
import org.junit.Rule

abstract class BaseInstrumentationTest {

    @get:Rule
    val disableAnimationsRule = DisableAnimationsRule()

    @get:Rule
    val taskExecutorRule = InstantTaskExecutorRule()
}
