/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.applifecycle

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import nl.rijksoverheid.en.config.AppConfigManager
import nl.rijksoverheid.en.resource.ResourceBundleManager

class EndOfLifeViewModel(
    private val resourceBundleManager: ResourceBundleManager,
    private val appConfigManager: AppConfigManager
) : ViewModel() {

    val endOfLifeContent: LiveData<Pair<String, String>?> = liveData {
        val appConfig = appConfigManager.getCachedConfigOrDefault()
        val result = resourceBundleManager
            .getEndOfLifeResources(appConfig.coronaMelderDeactivatedTitle, appConfig.coronaMelderDeactivatedBody)
        emit(result)
    }
}
