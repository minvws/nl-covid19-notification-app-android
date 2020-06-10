/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.factory

import android.content.Context
import com.google.android.gms.nearby.Nearby
import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.ExposureNotificationsRepository
import nl.rijksoverheid.en.api.ExposureNotificationService
import nl.rijksoverheid.en.enapi.NearbyExposureNotificationApi
import nl.rijksoverheid.en.job.ProcessManifestWorker
import nl.rijksoverheid.en.job.ProcessManifestWorkerScheduler
import nl.rijksoverheid.en.onboarding.OnboardingRepository

// cached service instance
private var service: ExposureNotificationService? = null

fun createExposureNotificationsRepository(context: Context): ExposureNotificationsRepository {
    val service = service ?: ExposureNotificationService.create(context).also { service = it }

    return ExposureNotificationsRepository(
        context,
        NearbyExposureNotificationApi(Nearby.getExposureNotificationClient(context)),
        service,
        context.getSharedPreferences("${BuildConfig.APPLICATION_ID}.notifications", 0),
        object : ProcessManifestWorkerScheduler {
            override fun schedule(intervalHours: Int) {
                ProcessManifestWorker.queue(context, intervalHours)
            }

            override fun cancel() {
                ProcessManifestWorker.cancel(context)
            }
        }
    )
}

fun createOnboardingRepository(context: Context): OnboardingRepository {
    return OnboardingRepository(
        context.getSharedPreferences("${BuildConfig.APPLICATION_ID}.onboarding", 0)
    )
}
