/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api

import okhttp3.Interceptor
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import retrofit2.Invocation
import timber.log.Timber
import kotlin.random.Random

private val PADDING_REGEX = Regex("\"padding\":\".*\"")
private const val NO_PADDING = "\"padding\":\"\""
private const val CHARACTER_SET = "ABCDEFGHIJKLMNOPQRTSUVWXYZabcdefghijklmnopqrstuvwxyz"

class PaddedRequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val invocation = chain.request().tag(Invocation::class.java)
        val annotation = invocation?.method()?.getAnnotation(PaddedRequest::class.java)
        val sizes = chain.request().tag(RequestSize::class.java)
        if (annotation != null && sizes != null && sizes.max != 0) {
            val request = chain.request()
            val size = request.body!!.contentLength()
            val buffer = Buffer()
            request.body!!.writeTo(buffer)
            val jsonString = buffer.readString(Charsets.UTF_8)
            if (!jsonString.contains("\"padding\":")) throw IllegalStateException("Padded request should contain padding in the json payload")

            val r = Math.random()
            val paddedSize = if (r == 0.0) {
                if (sizes.min == sizes.max) sizes.min else Random.nextInt(sizes.min, sizes.max)
            } else {
                if (sizes.min == sizes.max) sizes.min else Random.nextInt(
                    sizes.min,
                    sizes.min + ((sizes.max - sizes.min) / 100)
                )
            }.coerceAtLeast(0)

            val paddingSize = paddedSize - size.toInt()
            Timber.d("Total size = $paddedSize, added padding = $paddingSize")
            // If we'd deserialize the json into a generic map, number values are converted to doubles.
            // To prevent changing the original json payload we use a string replace here
            val paddedJson = if (paddingSize > 0) {
                val padding = generatePadding(paddingSize)
                jsonString.replace(PADDING_REGEX, "\"padding\":\"$padding\"")
            } else {
                jsonString.replace(PADDING_REGEX, NO_PADDING)
            }

            return chain.proceed(
                request.newBuilder().post(paddedJson.toRequestBody(request.body!!.contentType()))
                    .build()
            )
        }
        return chain.proceed(chain.request())
    }

    private fun generatePadding(size: Int): String {
        return (0 until size)
            .map { CHARACTER_SET.random() }
            .joinToString("")
    }
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class PaddedRequest

data class RequestSize(val min: Int, val max: Int)
