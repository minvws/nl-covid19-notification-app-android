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
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Tag

interface LabTestService {
    @POST("v1/register")
    suspend fun register(@Body request: RegistrationRequest): Registration

    @POST("v1/postkeys")
    @BodyHmacSha256Key
    suspend fun postKeys(
        @Body request: PostKeysRequest,
        @Tag hmacSecret: HmacSecret
    )

    companion object {
        fun create(
            context: Context,
            client: OkHttpClient = createOkHttpClient(context),
            baseUrl: String = BuildConfig.API_BASE_URL
        ): LabTestService {
            // Stub API implementation
            return object : LabTestService {
                override suspend fun register(request: RegistrationRequest): Registration {
                    return Registration("7C3-28C", "", ByteArray(0), 2400)
                }

                override suspend fun postKeys(request: PostKeysRequest, hmacSecret: HmacSecret) {
                    // stub
                }
            }
        }
    }
}
