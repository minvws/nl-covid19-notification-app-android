/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api

import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.en.api.model.PostKeysRequest
import nl.rijksoverheid.en.api.model.RegistrationRequest
import nl.rijksoverheid.en.api.model.TemporaryExposureKey
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.O_MR1])
class LabTestServiceTest {

    private val fakeAppVersionCode = 71407

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
            fakeAppVersionCode,
            baseUrl = mockWebServer.url("").toString()
        )
        mockWebServer.enqueue(MockResponse())

        api.postKeys(
            PostKeysRequest(listOf(), "bucketId"),
            HmacSecret("testId".toByteArray()),
            RequestSize(0, 0)
        )

        val request = mockWebServer.takeRequest()
        assertEquals(1, mockWebServer.requestCount)
        assertNotNull(request.requestUrl?.queryParameter("sig"))
        assertEquals(
            "4whsXhlI/s7PcF3UMl4L9ENJpmtMPm1ubht/euc4NQM=",
            request.requestUrl?.queryParameter("sig")
        )
    }

    @Test
    fun `postKeys converts padding to base64`() = runBlocking {
        val api = LabTestService.create(
            ApplicationProvider.getApplicationContext(),
            fakeAppVersionCode,
            baseUrl = mockWebServer.url("").toString()
        )
        mockWebServer.enqueue(MockResponse())

        api.postKeys(
            PostKeysRequest(
                listOf(),
                padding = byteArrayOf(1, 2, 3, 4),
                bucketId = "bucketId"
            ), HmacSecret("testId".toByteArray()),
            RequestSize(0, 0)
        )

        val request = mockWebServer.takeRequest()
        assertEquals(1, mockWebServer.requestCount)
        assertEquals(
            "{\"keys\":[],\"bucketId\":\"bucketId\",\"padding\":\"AQIDBA==\"}",
            String(request.body.readByteArray())
        )
    }

    @Test
    fun `postKeys adds padding to the request`() = runBlocking {
        val api = LabTestService.create(
            ApplicationProvider.getApplicationContext(),
            fakeAppVersionCode,
            baseUrl = mockWebServer.url("").toString()
        )
        mockWebServer.enqueue(MockResponse())

        api.postKeys(
            PostKeysRequest(
                listOf(TemporaryExposureKey(byteArrayOf(1, 2, 3), 1234, 1)),
                bucketId = "bucketId"
            ), HmacSecret("testId".toByteArray()),
            RequestSize(2000, 3000)
        )

        val request = mockWebServer.takeRequest()
        assertEquals(1, mockWebServer.requestCount)
        val jsonString = request.body.readString(Charsets.UTF_8)
        val json = Moshi.Builder().build().adapter<Map<String, Any?>>(
            Types.newParameterizedType(
                Map::class.java,
                String::class.java,
                Any::class.java
            )
        ).fromJson(jsonString)!!
        assertNotNull(json["padding"])
        val padding = json["padding"] as String
        assertTrue(padding.isNotBlank())
        // check that the message without the padding did not change
        assertEquals(
            "{\"keys\":[{\"keyData\":\"AQID\",\"rollingStartNumber\":1234,\"rollingPeriod\":1}],\"bucketId\":\"bucketId\"}",
            jsonString.replace(Regex(",\"padding\":\".*?\""), "")
        )
    }

    @Test
    fun `register adds padding to the request`() = runBlocking {
        val api = LabTestService.create(
            ApplicationProvider.getApplicationContext(),
            fakeAppVersionCode,
            baseUrl = mockWebServer.url("").toString()
        )
        mockWebServer.enqueue(MockResponse().setBody("{\"labConfirmationId\":\"server-code\",\"bucketId\":\"vPUC39ia6grsuAnpEEullKJTea6XBJC475EEKpZaD+I=\",\"confirmationKey\":\"I+dl3vS844SEZNYUZ1GDayU9yfGhN5oF0ae70q+Runk=\",\"validity\":64028}"))

        api.register(
            RegistrationRequest(),
            RequestSize(2000, 3000)
        )

        val request = mockWebServer.takeRequest()
        assertEquals(1, mockWebServer.requestCount)
        val json = Moshi.Builder().build().adapter<Map<String, Any?>>(
            Types.newParameterizedType(
                Map::class.java,
                String::class.java,
                Any::class.java
            )
        ).fromJson(request.body.readString(Charsets.UTF_8))!!
        assertNotNull(json["padding"])
        val padding = json["padding"] as String
        assertTrue(padding.isNotBlank())
    }
}
