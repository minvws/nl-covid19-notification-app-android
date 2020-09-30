/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api

import android.os.Build
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.O_MR1])
class UserAgentInterceptorTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `Check if the request contains the expected user-agent header`() {
        val expectedUserAgent =
            "CoronaMelder/" +
                "${BuildConfig.VERSION_CODE} " +
                "(${Build.MANUFACTURER} ${Build.MODEL}) " +
                "Android (${Build.VERSION.SDK_INT})"

        mockWebServer.enqueue(MockResponse().setBody("Test"))

        val okHttpBuilder = OkHttpClient.Builder()
        okHttpBuilder.addInterceptor(UserAgentInterceptor())

        val request: Request = Request.Builder().url(mockWebServer.url("/").toUrl()).build()
        val result = okHttpBuilder.build().newCall(request).execute().body

        assertNotNull(result)
        assertEquals("Test", result?.string())
        assertEquals(expectedUserAgent, mockWebServer.takeRequest().getHeader("User-Agent"))
    }
}
