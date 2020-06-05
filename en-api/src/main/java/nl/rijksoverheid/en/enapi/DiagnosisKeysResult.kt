/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.enapi

sealed class DiagnosisKeysResult {
    /**
     * Keys have been imported successfully
     */
    object Success : DiagnosisKeysResult()

    /**
     * Error handling the provided files, due to disk io errors.
     * Most commonly this means that the user has no storage left on the device.
     * This error is also reported when the files provided do not exist (anymore).
     */
    object FailedDiskIo : DiagnosisKeysResult()

    /**
     * An unexpected error occurred
     */
    data class UnknownError(val exception: Exception) : DiagnosisKeysResult()
}
