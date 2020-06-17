/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest

sealed class RequestKeyResult {
    /**
     * Returns the code to show to the user in the UI
     */
    data class Success(val code: String) : RequestKeyResult()

    /**
     * The code could not be retrieved due to an error
     */
    object UnknownError : RequestKeyResult()
}
