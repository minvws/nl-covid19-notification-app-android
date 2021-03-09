/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en

import android.content.Context
import androidx.lifecycle.ViewModel
import nl.rijksoverheid.en.factory.createAppConfigManager
import nl.rijksoverheid.en.factory.createResourceBundleManager
import nl.rijksoverheid.en.notification.PostNotificationViewModel

class TstViewModelFactory(context: Context) : ViewModelFactory(context) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when (modelClass) {
            PostNotificationViewModel::class.java -> PostNotificationViewModel(
                createResourceBundleManager(context),
                createAppConfigManager(context),
                TstEnApplication.useDefaultGuidance
            ) as T
            else -> super.create(modelClass)
        }
    }
}