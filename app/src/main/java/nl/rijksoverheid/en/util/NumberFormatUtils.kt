/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.util

import android.content.Context
import nl.rijksoverheid.en.R
import java.text.DecimalFormat
import java.util.Locale

fun Double.formatToString(context: Context): String {
    val locale = Locale(context.getString(R.string.app_language))
    return DecimalFormat.getInstance(locale).format(this).toString()
}

fun Float.formatPercentageToString(): String {
    return "${toString()}%"
}