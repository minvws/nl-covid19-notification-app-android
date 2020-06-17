/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest

import kotlinx.coroutines.delay

class LabTestRepository {
    suspend fun requestKey(): RequestKeyResult {
        delay(1000)
        return RequestKeyResult.Success("A56-34F")
    }

    fun uploadTeks() {
        // Schedule job
    }
}
