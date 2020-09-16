/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api

import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

/**
 * Interceptor that attempts to workaround an issue that seems to be related to a corrupted cache
 * entry. When the entry is read, [NullPointerException] can be throw.
 *
 * If that happens, try to evict the corrupt entry and retry the request.
 */
class CorruptedCacheInterceptor(private val cache: Cache) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return try {
            chain.proceed(chain.request())
        } catch (ex: NullPointerException) {
            Timber.w("Cache entry for ${chain.request().url} appears to be corrupted")
            val urls = cache.urls()
            for (url in urls) {
                if (url == chain.request().url.toString()) {
                    Timber.d("Evict url from cache")
                    urls.remove()
                    break
                }
            }
            chain.proceed(
                chain.request().newBuilder().cacheControl(CacheControl.FORCE_NETWORK).build()
            )
        }
    }
}
