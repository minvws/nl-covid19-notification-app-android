/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import androidx.annotation.StringRes
import nl.rijksoverheid.en.R

sealed class ErrorType(@StringRes val errorMessage: Int)

object DashboardServerError : ErrorType(R.string.dashboard_server_error)
