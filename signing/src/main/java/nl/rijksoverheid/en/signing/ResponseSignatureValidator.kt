/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.signing

import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x500.style.IETFUtils
import org.bouncycastle.cert.jcajce.JcaCertStoreBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
import org.bouncycastle.cms.CMSSignedDataParser
import org.bouncycastle.cms.CMSTypedStream
import org.bouncycastle.cms.SignerId
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import org.bouncycastle.util.encoders.Hex
import java.io.BufferedInputStream
import java.io.InputStream
import java.security.cert.CertPathBuilder
import java.security.cert.CertStore
import java.security.cert.CertificateFactory
import java.security.cert.PKIXBuilderParameters
import java.security.cert.PKIXCertPathBuilderResult
import java.security.cert.TrustAnchor
import java.security.cert.X509CertSelector
import java.security.cert.X509Certificate

// The publicly known default AuthorityKeyIdentifier for the issuer that issued the signing certificate
private val DEFAULT_AUTHORITY_KEY_IDENTIFIER =
    Hex.decode("30168014b8d44c9fa85b6eda25a7688eef8c461afe1f5365")

private val defaultRootCertificate: Lazy<X509Certificate> = lazy {
    val cf: CertificateFactory = CertificateFactory.getInstance("X.509")
    cf.generateCertificate(
        ResponseSignatureValidator::class.java.getResourceAsStream(
            "/PrivateRootCA-G1.cer"
        )
    ) as X509Certificate
}

class ResponseSignatureValidator(
    /* for testing */
    private val rootCertificate: Lazy<X509Certificate> = defaultRootCertificate,
    private val authorityKeyIdentifier: ByteArray = DEFAULT_AUTHORITY_KEY_IDENTIFIER
) {

    private val trustAnchor by lazy {
        TrustAnchor(rootCertificate.value, null)
    }
    private val provider = BouncyCastleProvider()

    fun verifySignature(content: InputStream, signature: ByteArray) {
        try {
            val sp = CMSSignedDataParser(
                JcaDigestCalculatorProviderBuilder().setProvider(provider)
                    .build(),
                CMSTypedStream(BufferedInputStream(content)),
                signature
            )

            sp.signedContent.drain()

            val certs = sp.certificates

            val store: CertStore =
                JcaCertStoreBuilder().setProvider(provider)
                    .addCertificate(JcaX509CertificateHolder(trustAnchor.trustedCert))
                    .addCertificates(certs)
                    .build()

            val signer =
                sp.signerInfos.signers.firstOrNull() ?: throw SignatureValidationException()
            val result = checkCertPath(trustAnchor, signer.sid, store)
                ?: throw SignatureValidationException()
            val signingCertificate = result.certPath.certificates[0] as X509Certificate

            if (!verifyCN(signingCertificate) ||
                !signer.verify(
                        JcaSimpleSignerInfoVerifierBuilder().setProvider(provider)
                            .build(signingCertificate)
                    )
            ) {
                throw SignatureValidationException()
            }
        } catch (ex: Throwable) {
            throw SignatureValidationException()
        }
    }

    private fun checkCertPath(
        trustAnchor: TrustAnchor,
        signerId: SignerId,
        certs: CertStore
    ): PKIXCertPathBuilderResult? {
        val pathBuilder: CertPathBuilder =
            CertPathBuilder.getInstance("PKIX", provider)
        val targetConstraints = X509CertSelector()

        // criteria to target the certificate to build the path to:
        // must match the signing certificate that we pass in, and the
        // signing certificate must have the correct authority key identifier
        targetConstraints.setIssuer(signerId.issuer.encoded)
        targetConstraints.serialNumber = signerId.serialNumber
        targetConstraints.authorityKeyIdentifier = authorityKeyIdentifier

        val params = PKIXBuilderParameters(
            setOf(trustAnchor),
            targetConstraints
        )

        params.addCertStore(certs)
        params.isRevocationEnabled = false
        return pathBuilder.build(params) as? PKIXCertPathBuilderResult
    }

    private fun verifyCN(signingCertificate: X509Certificate): Boolean {
        return JcaX509CertificateHolder(signingCertificate).subject.getRDNs(BCStyle.CN).any {
            val cn = IETFUtils.valueToString(it.first.value)
            cn.contains("CoronaMelder", true) && cn.endsWith(".nl")
        }
    }
}

class SignatureValidationException : RuntimeException()
