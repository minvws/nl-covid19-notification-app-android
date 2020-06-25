/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import retrofit2.Invocation
import java.util.zip.ZipInputStream

class SignedResponseInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val invocation = chain.request().tag(Invocation::class.java)
        val signed =
            invocation?.method()?.annotations?.filterIsInstance<SignedResponse>()?.firstOrNull()

        if (signed != null) {
            val enabled = BuildConfig.FEATURE_RESPONSE_SIGNATURES
            val request = if (BuildConfig.FEATURE_ACCEPT_HEADER) {
                chain.request().newBuilder()
                    .addHeader("Accept", if (enabled) "application/zip" else "application/json")
                    .build()
            } else {
                chain.request()
            }
            val response = chain.proceed(request)
            return if (response.isSuccessful && enabled) {
                validateAndRewriteResponse(response)
            } else {
                response
            }
        }

        return chain.proceed(chain.request())
    }

    private fun validateAndRewriteResponse(response: Response): Response {
        val body = response.body ?: throw IllegalStateException()
        val content = Buffer()
        val signature = Buffer()
        val input = ZipInputStream(body.byteStream())
        input.use {
            do {
                val entry = input.nextEntry ?: break
                if (entry.name == "content.bin") {
                    if (content.size > 0) {
                        throw IllegalStateException()
                    }
                    content.readFrom(it)
                } else if (entry.name == "content.sig") {
                    if (signature.size > 0) {
                        throw IllegalStateException()
                    }
                    signature.readFrom(it)
                }
                input.closeEntry()
            } while (true)
        }

        //TODO validate signature
        return response.newBuilder()
            .removeHeader("Content-Type")
            .body(content.readByteArray().toResponseBody("application/json".toMediaType())).build()
    }
}

/**
 * Annotation to mark that the body of a Retrofit request should be signed
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class SignedResponse
