/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest

import androidx.lifecycle.ViewModel
import nl.rijksoverheid.en.api.model.FeatureFlagOption
import nl.rijksoverheid.en.config.AppConfigManager

class LabTestDoneViewModel(
    private val appConfigManager: AppConfigManager
) : ViewModel() {

    suspend fun hasIndependentKeySharing() =
        appConfigManager.getCachedConfigOrDefault().hasFeature(FeatureFlagOption.INDEPENDENT_KEY_SHARING)
}
