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

class AcceptHeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val invocation = chain.request().tag(Invocation::class.java)
        val accept = invocation?.method()?.annotations?.filterIsInstance<Accept>()?.firstOrNull()
        val request = if (accept != null && accept.mimeType.isNotEmpty()) {
            val request = chain.request().newBuilder()
            request.addHeader("Accept", accept.mimeType)
            request.build()
        } else {
            chain.request()
        }

        return chain.proceed(request)
    }
}

/**
 * Annotation to mark that the body of a Retrofit request should be signed
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Accept(val mimeType: String)
