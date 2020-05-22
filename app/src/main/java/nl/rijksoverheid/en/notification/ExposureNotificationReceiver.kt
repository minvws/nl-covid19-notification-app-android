package nl.rijksoverheid.en.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import nl.rijksoverheid.en.MainActivity
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.factory.createExposureNotificationsRepository

class ExposureNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED) {
            val summary =
                intent.getParcelableExtra<ExposureSummary>(ExposureNotificationClient.EXTRA_EXPOSURE_SUMMARY)
            val repository = createExposureNotificationsRepository(context)
            val token = intent.getStringExtra(ExposureNotificationClient.EXTRA_TOKEN)
            if (summary != null && token != null) {
                repository.addExposure(token)
                showNotification(context)
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "exposure_notifications",
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = context.getString(R.string.notification_channel_description)
            val notificationManager: NotificationManager =
                context.getSystemService(NotificationManager::class.java)!!
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context) {
        createNotificationChannel(context)
        val intent =
            Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
        val builder =
            NotificationCompat.Builder(
                context,
                "exposure_notifications"
            )
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_message))
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.notification_message))
                )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true) // Do not reveal this notification on a secure lockscreen.
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
        val notificationManager =
            NotificationManagerCompat
                .from(context)
        notificationManager.notify(0, builder.build())
    }
}