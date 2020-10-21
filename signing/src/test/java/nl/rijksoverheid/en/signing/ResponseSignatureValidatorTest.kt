/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.signing

import org.bouncycastle.asn1.x509.SubjectKeyIdentifier
import org.bouncycastle.util.encoders.Hex
import org.junit.Test
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class ResponseSignatureValidatorTest {
    @Test
    fun `valid signature passes validation`() {
        val trustManager =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        // root key is on the device
        keyStore.load(
            ResponseSignatureValidatorTest::class.java.getResourceAsStream("/nl-root.jks"),
            "test".toCharArray()
        )
        trustManager.init(keyStore)

        val validator = ResponseSignatureValidator(
            trustManager = trustManager.trustManagers[0] as X509TrustManager
        )

        val signature =
            ResponseSignatureValidatorTest::class.java.getResourceAsStream("/content.sig")!!
                .readBytes()
        val target =
            ResponseSignatureValidatorTest::class.java.getResourceAsStream("/export.bin")!!

        validator.verifySignature(target, signature)
    }

    @Test(expected = SignatureValidationException::class)
    fun `invalid signature does not pass validation`() {
        val trustManager =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        // root key is on the device
        keyStore.load(
            ResponseSignatureValidatorTest::class.java.getResourceAsStream("/nl-root.jks"),
            "test".toCharArray()
        )
        trustManager.init(keyStore)

        val validator = ResponseSignatureValidator(
            trustManager = trustManager.trustManagers[0] as X509TrustManager
        )

        val signature =
            ResponseSignatureValidatorTest::class.java.getResourceAsStream("/content.sig")!!
                .readBytes()

        validator.verifySignature(ByteArrayInputStream("invalid".toByteArray()), signature)
    }

    @Test(expected = SignatureValidationException::class)
    fun `valid signature without root ca on device does not validate`() {
        val trustManager =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        // empty keystore
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        trustManager.init(keyStore)

        val validator = ResponseSignatureValidator(
            trustManager = trustManager.trustManagers[0] as X509TrustManager
        )

        val signature =
            ResponseSignatureValidatorTest::class.java.getResourceAsStream("/content.sig")!!
                .readBytes()
        val target =
            ResponseSignatureValidatorTest::class.java.getResourceAsStream("/export.bin")!!

        validator.verifySignature(target, signature)
    }

    @Test(expected = SignatureValidationException::class)
    fun `Signature with an invalid CN does not validate`() {
        val trustManager =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)

        // Load matching root cert
        val cf: CertificateFactory = CertificateFactory.getInstance("X.509")
        val caCert: X509Certificate = cf.generateCertificate(
            ResponseSignatureValidatorTest::class.java.getResourceAsStream(
                "/testroot.pem"
            )
        ) as X509Certificate

        keyStore.setCertificateEntry("TestRoot", caCert)
        trustManager.init(keyStore)

        val validator = ResponseSignatureValidator(
            trustManager = trustManager.trustManagers[0] as X509TrustManager,
            trustAnchorSubjectKeyIdentifier = SubjectKeyIdentifier(
                Hex.decode("04143ebd1363a152e330842def2c1869ca979073d062")
            ),
            authorityKeyIdentifier = Hex.decode("301680143ebd1363a152e330842def2c1869ca979073d062")
        )

        val signature =
            ResponseSignatureValidatorTest::class.java.getResourceAsStream("/CNTestfile.sig")!!
                .readBytes()
        val target =
            ResponseSignatureValidatorTest::class.java.getResourceAsStream("/CNTestfile.txt")!!

        validator.verifySignature(target, signature)
    }
}
