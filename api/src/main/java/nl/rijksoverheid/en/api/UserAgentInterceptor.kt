/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.api

import android.os.Build
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that modifies the user-agent so it contains the Device model,
 * OS version and App version.
 */
class UserAgentInterceptor(appVersionCode: Int) : Interceptor {

    companion object {
        private const val USER_AGENT = "User-Agent"
    }

    private val userAgent: String =
        "CoronaMelder/$appVersionCode (${Build.MANUFACTURER} ${Build.MODEL}) Android (${Build.VERSION.SDK_INT})"
            .stripNonAscii()

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestWithUserAgent = chain.request().newBuilder()
            .header(USER_AGENT, userAgent)
            .build()

        return chain.proceed(requestWithUserAgent)
    }

    private fun String.stripNonAscii() = filter {
        it == '\t' || it in '\u0020'..'\u007e'
    }
}
