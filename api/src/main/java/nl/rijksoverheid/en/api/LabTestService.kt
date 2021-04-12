/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api

import android.content.Context
import nl.rijksoverheid.en.api.model.PostKeysRequest
import nl.rijksoverheid.en.api.model.Registration
import nl.rijksoverheid.en.api.model.RegistrationRequest
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Tag

interface LabTestService {
    @POST("v2/register")
    @PaddedRequest
    suspend fun register(@Body request: RegistrationRequest, @Tag sizes: RequestSize): Registration

    @POST("v1/postkeys")
    @BodyHmacSha256Key
    @PaddedRequest
    suspend fun postKeys(
        @Body request: PostKeysRequest,
        @Tag hmacSecret: HmacSecret,
        @Tag requestSize: RequestSize
    )

    @POST("v1/stopkeys")
    @BodyHmacSha256Key
    @PaddedRequest
    suspend fun stopKeys(
        @Body request: PostKeysRequest,
        @Tag hmacSecret: HmacSecret,
        @Tag requestSize: RequestSize
    )

    companion object {
        fun create(
            context: Context,
            appVersionCode: Int,
            client: OkHttpClient = createOkHttpClient(context, appVersionCode),
            baseUrl: String = BuildConfig.API_BASE_URL
        ): LabTestService {
            return Retrofit.Builder()
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(createMoshi()))
                .baseUrl(baseUrl)
                .build().create(LabTestService::class.java)
        }
    }
}
