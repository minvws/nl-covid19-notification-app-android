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