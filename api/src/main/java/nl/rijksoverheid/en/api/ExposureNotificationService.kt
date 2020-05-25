/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import nl.rijksoverheid.en.api.model.TemporaryExposureKey
import nl.rijksoverheid.en.api.moshi.Base64JsonAdapter
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ExposureNotificationService {
    @POST("TemporaryExposureKey")
    suspend fun postTempExposureKeys(
        @Body request: List<TemporaryExposureKey>
    )

    @GET("TemporaryExposureKey")
    suspend fun getExposureKeysFile(@Query("filtered") filter: Boolean = true): Response<ResponseBody>

    companion object Factory {
        private var exposureNotificationService: ExposureNotificationService? = null

        private fun createMoshi() =
            Moshi.Builder()
                .add(Base64JsonAdapter())
                .add(KotlinJsonAdapterFactory()).build()

        private fun createClient(): OkHttpClient {
            val builder = OkHttpClient.Builder()
            builder.addInterceptor {
                it.proceed(
                    it.request().newBuilder()
                        .addHeader("Authorization", "Basic ${BuildConfig.API_KEY}").build()
                )
            }
            builder.addInterceptor(HttpLoggingInterceptor().apply {
                setLevel(HttpLoggingInterceptor.Level.BODY)
            })
            return builder.build()
        }

        val instance: ExposureNotificationService
            get() = exposureNotificationService ?: Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(createClient())
                .addConverterFactory(MoshiConverterFactory.create(createMoshi())).build()
                .create(ExposureNotificationService::class.java)
                .also { exposureNotificationService = it }
    }
}
