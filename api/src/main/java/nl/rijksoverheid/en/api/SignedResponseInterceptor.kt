/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api

import nl.rijksoverheid.en.signing.ResponseSignatureValidator
import nl.rijksoverheid.en.signing.SignatureValidationException
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import retrofit2.Invocation
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/**
 * Interceptor that will validate the response signature and rewrites the response body with json content
 */
class SignedResponseInterceptor : Interceptor {
    private val validator = ResponseSignatureValidator()

    override fun intercept(chain: Interceptor.Chain): Response {
        val invocation = chain.request().tag(Invocation::class.java)
        val signed =
            invocation?.method()?.annotations?.filterIsInstance<SignedResponse>()?.firstOrNull()

        if (signed != null) {
            val response = chain.proceed(chain.request())
            return if (response.isSuccessful) {
                validateAndRewriteResponse(response)
            } else {
                response
            }
        }

        return chain.proceed(chain.request())
    }

    private fun validateAndRewriteResponse(response: Response): Response {
        val (content, signature) = readContentAndSignature(response)

        return try {
            if (BuildConfig.FEATURE_RESPONSE_SIGNATURES) {
                validator.verifySignature(
                    ByteArrayInputStream(content.clone().readByteArray()),
                    signature.readByteArray()
                )
            }
            response.newBuilder()
                .removeHeader("Content-Type")
                .body(content.readByteArray().toResponseBody("application/json".toMediaType()))
                .build()
        } catch (ex: SignatureValidationException) {
            response.newBuilder().body("Signature failed to validate".toResponseBody()).code(500)
                .message("Signature failed to validate").build()
        }
    }

    /**
     * Read from the response the content.bin and content.sig entries and return the content and signature
     * as a buffer.
     */
    private fun readContentAndSignature(response: Response): Pair<Buffer, Buffer> {
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

        return Pair(content, signature)
    }
}

/**
 * Annotation to mark that the body of a Retrofit request should be signed
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class SignedResponse
