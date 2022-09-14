/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.applifecycle.service

import nl.rijksoverheid.en.api.CacheStrategy
import nl.rijksoverheid.en.api.CdnService
import nl.rijksoverheid.en.api.model.AppConfig
import nl.rijksoverheid.en.api.model.Manifest
import nl.rijksoverheid.en.api.model.ResourceBundle
import nl.rijksoverheid.en.api.model.RiskCalculationParameters
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * Stub implementation of [CdnService] that produces the appropriate response for the app
 * to show that is has been deactivated without hitting the server.
 */
class EndOfLifeCdnService : CdnService {
    override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
        throw NotImplementedError()
    }

    override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest {
        return Manifest(emptyList(), "", "end-of-life-id")
    }

    override suspend fun getRiskCalculationParameters(
        id: String,
        cacheStrategy: CacheStrategy?
    ): RiskCalculationParameters {
        throw NotImplementedError()
    }

    override suspend fun getAppConfig(id: String, cacheStrategy: CacheStrategy?): AppConfig {
        return AppConfig(coronaMelderDeactivated = "deactivated")
    }

    override suspend fun getResourceBundle(
        id: String,
        cacheStrategy: CacheStrategy?
    ): ResourceBundle {
        throw NotImplementedError()
    }
}
