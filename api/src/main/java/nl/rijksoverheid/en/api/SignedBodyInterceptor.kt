/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api

import android.util.Base64
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import retrofit2.Invocation
import retrofit2.http.Tag
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Interceptor that will add a hmac sha256 signature to the request when a
 * Retrofit method is annotated with [BodyHmacSha256Key].
 */
class SignedBodyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val invocation = chain.request().tag(Invocation::class.java)
        val annotation = invocation?.method()?.annotations
            ?.filterIsInstance<BodyHmacSha256Key>()
            ?.firstOrNull()
        val key = chain.request().tag(HmacSecret::class.java)

        return if (invocation != null && annotation != null && key != null) {
            chain.proceed(signRequest(chain.request(), annotation.query, key.secret))
        } else {
            chain.proceed(chain.request())
        }
    }

    private fun signRequest(request: Request, query: String, hmacSecret: String): Request {
        val buffer = Buffer()
        val body = request.body!!
        body.writeTo(buffer)
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey =
            SecretKeySpec(hmacSecret.toByteArray(), "HmacSHA256")
        mac.init(secretKey)
        val signature = mac.doFinal(buffer.clone().readByteArray())

        return request.newBuilder()
            .post(buffer.readByteString().toRequestBody(body.contentType()))
            .url(
                request.url.newBuilder().addQueryParameter(
                    query,
                    Base64.encodeToString(signature, Base64.NO_WRAP)
                ).build()
            )
            .build()
    }
}

/**
 * Annotation to mark that the body of a Retrofit request should be signed
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class BodyHmacSha256Key(
    /**
     * Query parameter that holds the signature. Defaults to `sig`
     **/
    val query: String = "sig"
)

/**
 * Class holding the hmac secret. Should be set as a [Tag] on the retrofit
 * method that is annotated with [BodyHmacSha256Key]
 */
data class HmacSecret(val secret: String)