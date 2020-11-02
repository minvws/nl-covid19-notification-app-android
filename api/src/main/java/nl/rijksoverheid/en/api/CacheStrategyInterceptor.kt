/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.api

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException

class CacheStrategyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return when (chain.request().tag(CacheStrategy::class.java)) {
            CacheStrategy.CACHE_FIRST -> {
                val cacheResponse = try {
                    chain.proceed(
                        chain.request().newBuilder().cacheControl(CacheControl.FORCE_CACHE).build()
                    )
                } catch (ex: IOException) {
                    Timber.w(ex, "Error getting response from cache")
                    null
                }
                return if (cacheResponse?.isSuccessful == true) {
                    cacheResponse
                } else {
                    Timber.d("Cache request failed, retry network")
                    chain.proceed(chain.request())
                }
            }
            CacheStrategy.CACHE_ONLY -> chain.proceed(
                chain.request().newBuilder().cacheControl(CacheControl.FORCE_CACHE).build()
            )
            CacheStrategy.CACHE_LAST -> {
                val response = try {
                    chain.proceed(chain.request())
                } catch (ex: IOException) {
                    Timber.d("Network request failed")
                    null
                }
                return if (response?.isSuccessful == true) {
                    response
                } else {
                    chain.proceed(
                        chain.request().newBuilder().cacheControl(CacheControl.FORCE_CACHE).build()
                    )
                }
            }
            else -> chain.proceed(chain.request())
        }
    }
}

enum class CacheStrategy {
    /**
     * Try cache first, network if not cached
     */
    CACHE_FIRST,

    /**
     * Try the cache only
     */
    CACHE_ONLY,

    /**
     * Try the cache if network fails or if the request is not successful
     */
    CACHE_LAST
}