/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.fragment.app.Fragment
import nl.rijksoverheid.en.BuildConfig

fun Context.isIgnoringBatteryOptimizations(): Boolean {
    return getSystemService(PowerManager::class.java)!!
        .isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)
}

@SuppressLint("BatteryLife")
fun Fragment.requestDisableBatteryOptimizations(requestCode: Int) {
    startActivityForResult(
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse("package:${BuildConfig.APPLICATION_ID}")),
        requestCode
    )
}