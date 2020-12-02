/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import nl.rijksoverheid.en.factory.createSettingsRepository

class ExposureNotificationsPausedBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val settings = createSettingsRepository(context)
            settings.rescheduleReminder()
        }
    }
}