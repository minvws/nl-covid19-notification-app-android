/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api

import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Invocation

/**
 * Interceptor that overrides the cache-control header when [CacheOverride] annotation is set.
 */
class CacheOverrideInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val invocation = chain.request().tag(Invocation::class.java)
        val annotation = invocation?.method()?.getAnnotation(CacheOverride::class.java)
        val response = chain.proceed(chain.request())
        return if (annotation != null) {
            if (response.isSuccessful && response.cacheResponse == null) {
                response.newBuilder().removeHeader("cache-control")
                    .addHeader("cache-control", annotation.cacheHeaderValue).build()
            } else {
                response
            }
        } else {
            response
        }
    }
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class CacheOverride(val cacheHeaderValue: String)
