/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import android.app.Application
import androidx.work.Configuration

class TestApplication : Application(), Configuration.Provider {
    /*
     * Provide on-demand configuration for work manager in robolectric tests
     * Using the real ENApplication class that initialises work manager eagerly will make tests
     * fail due initialising work manager multiple times.
     * */
    override fun getWorkManagerConfiguration(): Configuration = Configuration.Builder().build()
}
