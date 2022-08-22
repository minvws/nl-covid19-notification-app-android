/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
@file:Suppress("ktlint:filename")

package nl.rijksoverheid.en.api

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import nl.rijksoverheid.en.api.model.ResourceBundle
import nl.rijksoverheid.en.api.typeConverter.OffsetDateTimeConverter
import okhttp3.Cache
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.io.File

private var okHttpClient: OkHttpClient? = null

internal fun createMoshi() =
    Moshi.Builder()
        .add(
            PolymorphicJsonAdapterFactory.of(ResourceBundle.Guidance.Element::class.java, "type")
                .withSubtype(ResourceBundle.Guidance.Element.Paragraph::class.java, "paragraph")
                .withDefaultValue(ResourceBundle.Guidance.Element.Unknown)
        )
        .add(OffsetDateTimeConverter)
        .add(Base64Adapter())
        .add(KotlinJsonAdapterFactory())
        .build()

internal fun createOkHttpClient(context: Context, appVersionCode: Int): OkHttpClient {
    return okHttpClient ?: OkHttpClient.Builder()
        // enable cache for config and resource bundles
        .followRedirects(false)
        .apply {
            val cache = Cache(File(context.cacheDir, "http"), 32 * 1024 * 1024L)
            cache(cache)
            addInterceptor(CacheStrategyInterceptor())
            addNetworkInterceptor(CacheOverrideInterceptor())
            addNetworkInterceptor(SignedResponseInterceptor())
            addInterceptor(PaddedRequestInterceptor())
            addInterceptor(SignedBodyInterceptor())
            addInterceptor(UserAgentInterceptor(appVersionCode))
            if (Timber.forest().isNotEmpty()) {
                addInterceptor(
                    HttpLoggingInterceptor { message ->
                        Timber.tag("OkHttpClient").d(message)
                    }.setLevel(HttpLoggingInterceptor.Level.BODY)
                )
            }
            addInterceptor(CorruptedCacheInterceptor(cache))
            if (BuildConfig.FLAVOR != "dev") {
                connectionSpecs(
                    listOf(
                        ConnectionSpec.MODERN_TLS
                    )
                )
            }
        }.build().also { okHttpClient = it }
}
