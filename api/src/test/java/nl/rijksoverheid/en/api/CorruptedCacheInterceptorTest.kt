/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.charset.Charset
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.O_MR1])
class CorruptedCacheInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var cache: Cache
    private lateinit var context: Context
    private lateinit var tmpDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockWebServer = MockWebServer()
        mockWebServer.useHttps(createSslSocketFactory(), false)
        tmpDir = File.createTempFile("cache", "dir")
        tmpDir.delete()
        cache = Cache(tmpDir, 1024 * 1024)
    }

    private fun createSslSocketFactory(): SSLSocketFactory {
        val password = "mockwebserver".toCharArray()
        val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
        keystore.load(javaClass.getResourceAsStream("/mockwebserver.jks"), password)

        val kmfAlgorithm: String = KeyManagerFactory.getDefaultAlgorithm()
        val kmf: KeyManagerFactory = KeyManagerFactory.getInstance(kmfAlgorithm)
        kmf.init(keystore, password)

        val trustManagerFactory = TrustManagerFactory.getInstance(kmfAlgorithm)
        trustManagerFactory.init(keystore)

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(kmf.keyManagers, trustManagerFactory.trustManagers, null)
        return sslContext.socketFactory
    }

    @Test(expected = NullPointerException::class)
    // if this test starts failing, the OkHttp bug might have been fixed
    fun `OkHttp crashes when certificates in cache entry are corrupted`() {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("cache-control", "public, max-age=${Int.MAX_VALUE}")
        )
        mockWebServer.start()
        val url = mockWebServer.url("/").toString()
        createCorruptedCacheEntry(mockWebServer.url(url).toString())

        val client = createClientBuilder().cache(cache).build()
        client.newCall(Request.Builder().url(url).build()).execute()
    }

    @Test
    fun `CorruptedCacheInterceptor retries network if cache entry is corrupted`() {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("cache-control", "public, max-age=${Int.MAX_VALUE}")
                .setBody("Cached")
        )
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("cache-control", "public, max-age=${Int.MAX_VALUE}")
                .setBody("Network")
        )
        mockWebServer.start()

        val url = mockWebServer.url("/").toString()
        createCorruptedCacheEntry(mockWebServer.url(url).toString())

        val client =
            createClientBuilder().cache(cache).addInterceptor(CorruptedCacheInterceptor(cache))
                .build()

        val response = client.newCall(Request.Builder().url(url).build()).execute().body?.string()

        assertEquals(2, mockWebServer.requestCount)
        assertEquals("Network", response)
    }

    private fun createClientBuilder(): OkHttpClient.Builder {
        // trust all certs, hosts client
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return emptyArray()
            }
        }

        return OkHttpClient.Builder().sslSocketFactory(
            SSLContext.getInstance("TLS").apply {
                init(null, arrayOf(trustManager), null)
            }.socketFactory,
            trustManager
        ).hostnameVerifier(HostnameVerifier { _, _ -> true })
    }

    private fun createCorruptedCacheEntry(url: String) {
        cache.evictAll()
        val client =
            createClientBuilder().cache(cache)
                .build()
        client.newCall(Request.Builder().url(url).build()).execute().use {
            // read the body
            it.body?.string()
        }

        val file = tmpDir.listFiles().first { it.name != "journal" && it.name.endsWith(".0") }
        val content = file.readBytes().toString(Charset.defaultCharset())
        file.writeText(
            """
                    $url
                    GET
                    0
                    HTTP/1.1 200 OK
                    4
                    Content-Length: 0
                    cache-control: public, max-age=2147483647
                    OkHttp-Sent-Millis: 1600177861920
                    OkHttp-Received-Millis: 1600177861929

                    TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA
                    1
                    *%^INVALID&*
                    0
                    TLSv1.2
            """.trimIndent()
        )
    }
}
