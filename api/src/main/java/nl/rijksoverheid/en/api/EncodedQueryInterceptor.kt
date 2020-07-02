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

class EncodedQueryInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val invocation = chain.request().tag(Invocation::class.java)
        val annotation = invocation?.method()?.getAnnotation(EncodedQuery::class.java)
        return if (annotation != null && annotation.encodedQuery.isNotBlank()) {
            chain.proceed(
                chain.request().newBuilder().url(
                    chain.request().url.newBuilder().encodedQuery(annotation.encodedQuery).build()
                ).build()
            )
        } else {
            chain.proceed(chain.request())
        }
    }
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class EncodedQuery(val encodedQuery: String)
