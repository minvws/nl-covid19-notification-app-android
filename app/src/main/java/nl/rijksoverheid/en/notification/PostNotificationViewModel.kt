/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.notification

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import nl.rijksoverheid.en.config.AppConfigManager
import nl.rijksoverheid.en.resource.ResourceBundleManager
import java.time.LocalDate

class PostNotificationViewModel(
    private val resourceBundleManager: ResourceBundleManager,
    private val appConfigManager: AppConfigManager,
    private val useLocalGuidance: Boolean = false
) : ViewModel() {

    private val exposureDate = MutableLiveData<LocalDate>()

    val guidance = exposureDate.switchMap {
        liveData {
            emit(resourceBundleManager.getExposureNotificationGuidance(it, useLocalGuidance))
        }
    }

    suspend fun getAppointmentPhoneNumber() =
        appConfigManager.getCachedConfigOrDefault().appointmentPhoneNumber

    fun setExposureDate(exposureDate: LocalDate) {
        this.exposureDate.value = exposureDate
    }
}
