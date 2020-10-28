/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.fragment.app.Fragment
import nl.rijksoverheid.en.BuildConfig

@SuppressLint("BatteryLife")
private val requestDisableBatteryOptimizationsIntent =
    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse("package:${BuildConfig.APPLICATION_ID}"))

/**
 * Returns if battery optimizations are ignored.
 * @return true if ignored *or* if there's no activity available to disable the setting
 */
fun Context.isIgnoringBatteryOptimizations(): Boolean {
    return if (supportsRequestDisableBatteryOptimisations(this)) {
        (getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)
            ?: true)
    } else {
        true
    }
}

private fun supportsRequestDisableBatteryOptimisations(context: Context): Boolean {
    return context.packageManager.resolveActivity(
        requestDisableBatteryOptimizationsIntent,
        PackageManager.MATCH_DEFAULT_ONLY
    ) != null
}

fun Fragment.requestDisableBatteryOptimizations(requestCode: Int) {
    startActivityForResult(
        requestDisableBatteryOptimizationsIntent,
        requestCode
    )
}
