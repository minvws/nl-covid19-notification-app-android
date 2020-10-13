/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.notification

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import nl.rijksoverheid.en.resource.ResourceBundleManager
import java.time.LocalDate

class PostNotificationViewModel(private val resourceBundleManager: ResourceBundleManager) :
    ViewModel() {

    private val exposureDate = MutableLiveData<LocalDate>()

    val guidance = exposureDate.switchMap {
        liveData {
            emit(resourceBundleManager.getExposureNotificationGuidance(it))
        }
    }

    fun setExposureDate(exposureDate: LocalDate) {
        this.exposureDate.value = exposureDate
    }
}