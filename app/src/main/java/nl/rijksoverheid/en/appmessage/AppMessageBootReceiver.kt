/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.appmessage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import nl.rijksoverheid.en.factory.RepositoryFactory

/**
 * BroadcastReceiver for rescheduling [AppMessageReceiver] when device is booted.
 */
class AppMessageBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val appConfigManager = RepositoryFactory.createAppConfigManager(context)

            val async = goAsync()
            MainScope().launch {
                appConfigManager.getCachedConfigOrDefault().notification?.let{ appMessage ->
                    AppMessageReceiver.schedule(context, appMessage.scheduledDateTime.toLocalDateTime())
                }
                async.finish()
            }
        }
    }
}