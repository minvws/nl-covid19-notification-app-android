/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.signing

import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x500.style.IETFUtils
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier
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
import java.security.KeyStore
import java.security.cert.CertPathBuilder
import java.security.cert.CertStore
import java.security.cert.PKIXBuilderParameters
import java.security.cert.PKIXCertPathBuilderResult
import java.security.cert.TrustAnchor
import java.security.cert.X509CertSelector
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

// The publicly known default SubjectKeyIdentifier for the root CA, retrieved from the device trust store
private val DEFAULT_ANCHOR_SUBJECT_KEY_IDENTIFIER =
    SubjectKeyIdentifier(Hex.decode("0414feab0090989e24fca9cc1a8afb27b8bf306ea83b"))

// The publicly known default AuthorityKeyIdentifier for the issuer that issued the signing certificate
private val DEFAULT_AUTHORITY_KEY_IDENTIFIER =
    Hex.decode("30168014084aaabb99246fbe5b07f1a58a995b2d47efb93c")

class ResponseSignatureValidator(
    trustManager: X509TrustManager = getDefaultTrustManager(),
    trustAnchorSubjectKeyIdentifier: SubjectKeyIdentifier = DEFAULT_ANCHOR_SUBJECT_KEY_IDENTIFIER,
    private val authorityKeyIdentifier: ByteArray = DEFAULT_AUTHORITY_KEY_IDENTIFIER,
    private val validateCN: (cn: String) -> Boolean = ::validateCN
) {

    private val trustAnchor: TrustAnchor?
    private val provider = BouncyCastleProvider()

    init {
        trustAnchor = getCertificateForSubjectKeyIdentifier(
            trustManager,
            trustAnchorSubjectKeyIdentifier
        )?.let {
            TrustAnchor(it, null)
        }
    }

    private fun getCertificateForSubjectKeyIdentifier(
        trustManager: X509TrustManager,
        subjectKeyIdentifier: SubjectKeyIdentifier
    ): X509Certificate? {
        return trustManager.acceptedIssuers.firstOrNull { certificate ->
            val ski = certificate.getExtensionValue(Extension.subjectKeyIdentifier.id)
                ?.let { SubjectKeyIdentifier.getInstance(it)?.keyIdentifier }
            ski?.contentEquals(subjectKeyIdentifier.keyIdentifier) == true
        }
    }

    fun verifySignature(content: InputStream, signature: ByteArray) {
        val trustAnchor = this.trustAnchor ?: throw SignatureValidationException()

        try {
            val sp = CMSSignedDataParser(
                JcaDigestCalculatorProviderBuilder().setProvider(provider)
                    .build(), CMSTypedStream(BufferedInputStream(content)), signature
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
        } catch (ex: Exception) {
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
        return pathBuilder.build(params) as PKIXCertPathBuilderResult
    }

    private fun verifyCN(signingCertificate: X509Certificate): Boolean {
        return JcaX509CertificateHolder(signingCertificate).subject.getRDNs(BCStyle.CN).any {
            val cn = IETFUtils.valueToString(it.first.value)
            validateCN(cn)
        }
    }
}

private fun getDefaultTrustManager(): X509TrustManager {
    val algorithm = TrustManagerFactory.getDefaultAlgorithm()
    val tm = TrustManagerFactory.getInstance(algorithm)
    @Suppress("CAST_NEVER_SUCCEEDS")
    tm.init(null as? KeyStore)
    return tm.trustManagers[0] as X509TrustManager
}

private fun validateCN(cn: String) =
    cn.contains("CoronaMelder", true) && cn.endsWith(".nl")

class SignatureValidationException : RuntimeException()
