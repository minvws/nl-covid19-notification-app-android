/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.api

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File

private const val MANIFEST_RESPONSE = """{
  "exposureKeySets": [],
  "riskCalculationParameters": "test",
  "appConfig": "test"
}"""

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.O_MR1])
class CacheStrategyInterceptorTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var cache: Cache
    private lateinit var context: Context
    private lateinit var tmpDir: File
    private lateinit var cdnService: CdnService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockWebServer = MockWebServer()
        tmpDir = File.createTempFile("cache", "dir")
        tmpDir.delete()
        cache = Cache(tmpDir, 1024 * 1024)
        cdnService = Retrofit.Builder().client(
            OkHttpClient.Builder().cache(cache).addInterceptor(CacheStrategyInterceptor()).build()
        ).baseUrl(mockWebServer.url("/")).addConverterFactory(
            MoshiConverterFactory.create(
                createMoshi()
            )
        ).build().create(CdnService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `cacheStrategy CACHE_FIRST will try the cache first, then network`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody(MANIFEST_RESPONSE))
        val manifest = cdnService.getManifest()

        val cachedManifest = cdnService.getManifest(CacheStrategy.CACHE_FIRST)
        assertEquals(manifest, cachedManifest)
        assertEquals(1, mockWebServer.requestCount)
    }

    @Test
    fun `cacheStrategy CACHE_LAST will try the cache in case of an error response`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody(MANIFEST_RESPONSE))
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        val manifest = cdnService.getManifest()

        val cachedManifest = cdnService.getManifest(CacheStrategy.CACHE_LAST)
        assertEquals(manifest, cachedManifest)
        assertEquals(2, mockWebServer.requestCount)
    }

    @Test
    fun `cacheStrategy CACHE_ONLY will try the cache the cache only`() = runBlocking {
        try {
            cdnService.getManifest(CacheStrategy.CACHE_ONLY)
            fail("Exception expected")
        } catch (ex: HttpException) {
            assertEquals(504, ex.code())
        }
        assertEquals(0, mockWebServer.requestCount)
    }
}