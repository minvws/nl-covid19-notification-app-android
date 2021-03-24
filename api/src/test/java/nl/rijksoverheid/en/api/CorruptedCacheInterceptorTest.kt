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
import okhttp3.CipherSuite
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.TlsVersion
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.security.cert.X509Certificate

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.O_MR1])
class CorruptedCacheInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var cache: Cache
    private lateinit var context: Context
    private lateinit var tmpDir: File
    private lateinit var serverCertificate: X509Certificate

    @Before
    fun setup() {
        val certificate = HeldCertificate.Builder()
            .addSubjectAlternativeName("localhost")
            .rsa2048()
            .build()
        val handshakeCertificate = HandshakeCertificates.Builder()
            .heldCertificate(certificate)
            .build()

        serverCertificate = certificate.certificate

        context = ApplicationProvider.getApplicationContext()
        mockWebServer = MockWebServer()
        mockWebServer.useHttps(handshakeCertificate.sslSocketFactory(), false)
        tmpDir = File.createTempFile("cache", "dir")
        tmpDir.delete()
        cache = Cache(tmpDir, 1024 * 1024)
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
        val handshakeCertificate =
            HandshakeCertificates.Builder().addTrustedCertificate(serverCertificate).build()

        // specify connection specs explicitly and disable TLS 1.2 and 1.3 for this test
        // since this seems to fail on JDK 11 (with AGP 4.1.0)
        val spec = ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS)
            .tlsVersions(TlsVersion.TLS_1_0, TlsVersion.TLS_1_1)
            .cipherSuites(
                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
                CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
                CipherSuite.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256,
                CipherSuite.TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256,
                CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,
                CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,
                CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256,
                CipherSuite.TLS_RSA_WITH_AES_256_GCM_SHA384,
                CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA,
                CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA,
                CipherSuite.TLS_RSA_WITH_3DES_EDE_CBC_SHA
            ).build()

        return OkHttpClient.Builder().sslSocketFactory(
            handshakeCertificate.sslSocketFactory(),
            handshakeCertificate.trustManager
        ).connectionSpecs(listOf(spec))
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

        val file = tmpDir.listFiles()!!.first { it.name != "journal" && it.name.endsWith(".0") }
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
