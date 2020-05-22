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

fun createExposureNotificationsRepository(context: Context): ExposureNotificationsRepository {
    return ExposureNotificationsRepository(
        context,
        Nearby.getExposureNotificationClient(context),
        ExposureNotificationService.instance,
        context.getSharedPreferences("${BuildConfig.APPLICATION_ID}.notifications", 0)
    )
}
