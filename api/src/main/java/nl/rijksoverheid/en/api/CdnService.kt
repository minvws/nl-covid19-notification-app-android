/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api

import android.content.Context
import nl.rijksoverheid.en.api.model.AppConfig
import nl.rijksoverheid.en.api.model.Manifest
import nl.rijksoverheid.en.api.model.ResourceBundle
import nl.rijksoverheid.en.api.model.RiskCalculationParameters
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming
import retrofit2.http.Tag

interface CdnService {
    @GET("v5/exposurekeyset/{id}")
    @Streaming
    @CacheOverride("no-store")
    suspend fun getExposureKeySetFile(@Path("id") id: String): Response<ResponseBody>

    @GET("v5/manifest")
    @SignedResponse
    suspend fun getManifest(@Tag cacheStrategy: CacheStrategy? = null): Manifest

    @GET("v5/riskcalculationparameters/{id}")
    @SignedResponse
    suspend fun getRiskCalculationParameters(
        @Path("id") id: String,
        @Tag cacheStrategy: CacheStrategy? = null
    ): RiskCalculationParameters

    @GET("v5/appconfig/{id}")
    @SignedResponse
    suspend fun getAppConfig(
        @Path("id") id: String,
        @Tag cacheStrategy: CacheStrategy? = null
    ): AppConfig

    @GET("v5/resourcebundle/{id}")
    @SignedResponse
    suspend fun getResourceBundle(
        @Path("id") id: String,
        @Tag cacheStrategy: CacheStrategy? = null
    ): ResourceBundle

    companion object {
        fun create(
            context: Context,
            appVersionCode: Int,
            client: OkHttpClient = createOkHttpClient(context, appVersionCode),
            baseUrl: String = BuildConfig.CDN_BASE_URL
        ): CdnService {
            return Retrofit.Builder()
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(createMoshi()))
                .baseUrl(baseUrl)
                .build().create(CdnService::class.java)
        }
    }
}
