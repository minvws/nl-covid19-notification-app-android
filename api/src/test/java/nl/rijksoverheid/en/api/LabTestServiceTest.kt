/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api

import android.os.Build
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.en.api.model.PostKeysRequest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.O_MR1])
class LabTestServiceTest {

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
    fun `postKeys with interceptor signs request`() = runBlocking {
        val api = LabTestService.create(
            ApplicationProvider.getApplicationContext(),
            baseUrl = mockWebServer.url("").toString()
        )
        mockWebServer.enqueue(MockResponse())

        api.postKeys(PostKeysRequest(listOf(), "bucketId"), HmacSecret("testId".toByteArray()))

        val request = mockWebServer.takeRequest()
        assertEquals(1, mockWebServer.requestCount)
        assertNotNull(request.requestUrl?.queryParameter("sig"))
        assertEquals(
            "p976MC558v8I9/v7AoikG1RglvKbNS7PWj/iWDjSPQg=",
            request.requestUrl?.queryParameter("sig")
        )
    }

    @Test
    fun `postKeys converts padding to base64`() = runBlocking {
        val api = LabTestService.create(
            ApplicationProvider.getApplicationContext(),
            baseUrl = mockWebServer.url("").toString()
        )
        mockWebServer.enqueue(MockResponse())

        api.postKeys(
            PostKeysRequest(
                listOf(),
                padding = byteArrayOf(1, 2, 3, 4),
                bucketId = "bucketId"
            ), HmacSecret("testId".toByteArray())
        )

        val request = mockWebServer.takeRequest()
        assertEquals(1, mockWebServer.requestCount)
        assertEquals(
            "{\"keys\":[],\"bucketId\":\"bucketId\",\"padding\":\"AQIDBA==\"}",
            String(request.body.readByteArray())
        )
    }
}
