/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import androidx.core.text.BidiFormatter
import androidx.core.text.TextDirectionHeuristicsCompat

fun String.forceLtr(): String =
    BidiFormatter.getInstance().unicodeWrap(this, TextDirectionHeuristicsCompat.LTR, true)
