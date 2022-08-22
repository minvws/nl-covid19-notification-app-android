/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.signing

import org.bouncycastle.util.encoders.Hex
import org.junit.Test
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class ResponseSignatureValidatorTest {
    @Test
    fun `valid signature passes validation`() {
        val validator = ResponseSignatureValidator()

        val signature =
            ResponseSignatureValidatorTest::class.java.getResourceAsStream("/content.sig")!!
                .readBytes()
        val target =
            ResponseSignatureValidatorTest::class.java.getResourceAsStream("/content.bin")!!

        validator.verifySignature(target, signature)
    }

    @Test(expected = SignatureValidationException::class)
    fun `invalid signature does not pass validation`() {
        val validator = ResponseSignatureValidator()

        val signature =
            ResponseSignatureValidatorTest::class.java.getResourceAsStream("/content.sig")!!
                .readBytes()

        validator.verifySignature(ByteArrayInputStream("invalid".toByteArray()), signature)
    }

    @Test(expected = SignatureValidationException::class)
    fun `Signature with an invalid CN does not validate`() {
        // root certificate for test file
        val cf: CertificateFactory = CertificateFactory.getInstance("X.509")
        val caCert: X509Certificate = cf.generateCertificate(
            ResponseSignatureValidatorTest::class.java.getResourceAsStream(
                "/testroot.pem"
            )
        ) as X509Certificate

        val validator = ResponseSignatureValidator(
            lazy { caCert },
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
