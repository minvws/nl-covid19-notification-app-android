/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.onboarding

import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import nl.rijksoverheid.en.ExposureNotificationsViewModel
import nl.rijksoverheid.en.ignoreInitiallyEnabled
import nl.rijksoverheid.en.util.launchDisableBatteryOptimizationsRequest

class OnboardingHelper(fragment: Fragment) {
    private val onboardingViewModel by fragment.activityViewModels<OnboardingViewModel>()
    private val exposureNotificationsViewModel by fragment.activityViewModels<ExposureNotificationsViewModel>()
    private val notificationsPermissionRequest =
        fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // always continue, the main screen has a warning
            requestDisableBatteryOptimizations()
        }

    private val disableBatteryOptimizationsRequest =
        fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { onboardingViewModel.continueOnboarding() }

    fun observeExposureNotificationsApiEnabled(viewLifecycleOwner: LifecycleOwner) {
        exposureNotificationsViewModel.notificationState.ignoreInitiallyEnabled()
            .observe(viewLifecycleOwner) {
                if (it is ExposureNotificationsViewModel.NotificationsState.Enabled) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        requestDisableBatteryOptimizations()
                    } else {
                        notificationsPermissionRequest.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
    }

    fun requestDisableBatteryOptimizations() {
        disableBatteryOptimizationsRequest.launchDisableBatteryOptimizationsRequest { onboardingViewModel.continueOnboarding() }
    }
}
