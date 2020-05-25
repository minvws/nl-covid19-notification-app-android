/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import nl.rijksoverheid.en.factory.createExposureNotificationsRepository

class ViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val context = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when (modelClass) {
            ExposureNotificationsViewModel::class.java -> ExposureNotificationsViewModel(
                createExposureNotificationsRepository(context)
            ) as T
            else -> throw IllegalStateException("Unknown view model class $modelClass")
        }
    }
}
