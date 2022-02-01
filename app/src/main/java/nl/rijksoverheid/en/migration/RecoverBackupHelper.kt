/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.migration

import android.content.Context
import android.os.Build
import nl.rijksoverheid.en.job.CheckConnectionWorker
import timber.log.Timber
import java.io.File

object RecoverBackupHelper {

    /**
     * Restore EncryptedSharedPreferences when unable to decrypt. This can happen when a user uses
     * device to device migration. After migration the new device doesn't contain the keys to decrypt.
     * In this case we remove the Encrypted preference file (it will automatically be recreated).
     * And schedule a notification so the user is aware that CoronaMelder needs to be turned on.
     */
    fun recoverSecurePreferencesFromBackupMigration(context: Context, fileName: String) {
        Timber.d("Delete SharedPreferences")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.deleteSharedPreferences(fileName)
        } else {
            val prefsFile = File(File(context.applicationInfo.dataDir, "shared_prefs"), "$fileName.xml")
            prefsFile.delete()
        }
        Timber.d("Schedule CheckConnectionWorker")
        CheckConnectionWorker.queue(context)
    }
}
