/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.resource

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.en.api.CacheStrategy
import nl.rijksoverheid.en.api.CdnService
import nl.rijksoverheid.en.api.model.AppConfig
import nl.rijksoverheid.en.api.model.Manifest
import nl.rijksoverheid.en.api.model.ResourceBundle
import nl.rijksoverheid.en.api.model.RiskCalculationParameters
import nl.rijksoverheid.en.util.formatDaysSince
import nl.rijksoverheid.en.util.formatExposureDate
import nl.rijksoverheid.en.util.formatExposureDateShort
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class ResourceBundleManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `getExposureNotificationGuidance replaces escaped new lines`() = runBlocking {
        val service = FakeResourceBundleCdnService(
            ResourceBundle(
                mapOf("en" to mapOf("test_key" to "value with a new line\nand with an escaped newline \\\ntoo")),
                ResourceBundle.Guidance(
                    5,
                    listOf(ResourceBundle.Guidance.Element.Paragraph("test_key", "test_key"))
                )
            )
        )

        val resourceBundleManager = ResourceBundleManager(context, service, useDefaultGuidance = false)

        val result = resourceBundleManager.getExposureNotificationGuidance(LocalDate.now())

        val paragraph = result[0] as ResourceBundle.Guidance.Element.Paragraph
        assertEquals(
            "value with a new line\n" +
                "and with an escaped newline \n" +
                "too",
            paragraph.title
        )
        assertEquals(
            "value with a new line\n" +
                "and with an escaped newline \n" +
                "too",
            paragraph.body
        )
    }

    @Test
    fun `getExposureNotificationGuidance replaces ExposureDate place holder`() = runBlocking {
        val service = FakeResourceBundleCdnService(
            ResourceBundle(
                mapOf("en" to mapOf("test_key" to "value with a {ExposureDate}")),
                ResourceBundle.Guidance(
                    5,
                    listOf(ResourceBundle.Guidance.Element.Paragraph("test_key", "test_key"))
                )
            )
        )

        val resourceBundleManager = ResourceBundleManager(context, service, useDefaultGuidance = false)
        val date = LocalDate.of(2020, 11, 2)

        val result = resourceBundleManager.getExposureNotificationGuidance(date)
        val paragraph = result[0] as ResourceBundle.Guidance.Element.Paragraph

        val formattedDate = date.formatExposureDate(context)

        assertEquals("value with a $formattedDate", paragraph.title)
        assertEquals("value with a $formattedDate", paragraph.body)
    }

    @Test
    fun `getExposureNotificationGuidance replaces ExposureDate place holder with offset`() =
        runBlocking {
            val service = FakeResourceBundleCdnService(
                ResourceBundle(
                    mapOf("en" to mapOf("test_key" to "value with a {ExposureDate+2}")),
                    ResourceBundle.Guidance(
                        5,
                        listOf(ResourceBundle.Guidance.Element.Paragraph("test_key", "test_key"))
                    )
                )
            )

            val resourceBundleManager = ResourceBundleManager(context, service, useDefaultGuidance = false)
            val date = LocalDate.of(2020, 11, 2)

            val result = resourceBundleManager.getExposureNotificationGuidance(date)
            val paragraph = result[0] as ResourceBundle.Guidance.Element.Paragraph

            val formattedDate = date.plusDays(2).formatExposureDate(context)

            assertEquals("value with a $formattedDate", paragraph.title)
            assertEquals("value with a $formattedDate", paragraph.body)
        }

    @Test
    fun `getExposureNotificationGuidance replaces ExposureDateShort place holder`() = runBlocking {
        val service = FakeResourceBundleCdnService(
            ResourceBundle(
                mapOf("en" to mapOf("test_key" to "value with a {ExposureDateShort}")),
                ResourceBundle.Guidance(
                    5,
                    listOf(ResourceBundle.Guidance.Element.Paragraph("test_key", "test_key"))
                )
            )
        )

        val resourceBundleManager = ResourceBundleManager(context, service, useDefaultGuidance = false)
        val date = LocalDate.of(2020, 11, 2)

        val result = resourceBundleManager.getExposureNotificationGuidance(date)
        val paragraph = result[0] as ResourceBundle.Guidance.Element.Paragraph

        val formattedDate = date.formatExposureDateShort(context)

        assertEquals("value with a $formattedDate", paragraph.title)
        assertEquals("value with a $formattedDate", paragraph.body)
    }

    @Test
    fun `getExposureNotificationGuidance replaces ExposureDateShort place holder with offset`() =
        runBlocking {
            val service = FakeResourceBundleCdnService(
                ResourceBundle(
                    mapOf("en" to mapOf("test_key" to "value with a {ExposureDateShort+5}")),
                    ResourceBundle.Guidance(
                        5,
                        listOf(ResourceBundle.Guidance.Element.Paragraph("test_key", "test_key"))
                    )
                )
            )

            val resourceBundleManager = ResourceBundleManager(context, service, useDefaultGuidance = false)
            val date = LocalDate.of(2020, 11, 2)

            val result = resourceBundleManager.getExposureNotificationGuidance(date)
            val paragraph = result[0] as ResourceBundle.Guidance.Element.Paragraph

            val formattedDate = date.plusDays(5).formatExposureDateShort(context)

            assertEquals("value with a $formattedDate", paragraph.title)
            assertEquals("value with a $formattedDate", paragraph.body)
        }

    @Test
    fun `getExposureNotificationGuidance replaces ExposureDaysAgo place holder`() = runBlocking {
        val service = FakeResourceBundleCdnService(
            ResourceBundle(
                mapOf("en" to mapOf("test_key" to "value with a {ExposureDaysAgo}")),
                ResourceBundle.Guidance(
                    5,
                    listOf(ResourceBundle.Guidance.Element.Paragraph("test_key", "test_key"))
                )
            )
        )

        val clock = Clock.fixed(
            LocalDate.of(2020, 11, 3).atStartOfDay().toInstant(ZoneOffset.UTC),
            ZoneId.of("UTC")
        )
        val resourceBundleManager = ResourceBundleManager(context, service, clock, false)
        val date = LocalDate.of(2020, 11, 2)

        val result = resourceBundleManager.getExposureNotificationGuidance(date)
        val paragraph = result[0] as ResourceBundle.Guidance.Element.Paragraph

        val daysAgo = date.formatDaysSince(context, clock)

        assertEquals("value with a $daysAgo", paragraph.title)
        assertEquals("value with a $daysAgo", paragraph.body)
    }

    @Test
    fun `getExposureNotificationGuidance replaces StayHomeUntilDate place holder based on quarantine days`() =
        runBlocking {
            val service = FakeResourceBundleCdnService(
                ResourceBundle(
                    mapOf("en" to mapOf("test_key" to "value with a {StayHomeUntilDate}")),
                    ResourceBundle.Guidance(
                        5,
                        listOf(ResourceBundle.Guidance.Element.Paragraph("test_key", "test_key"))
                    )
                )
            )

            val resourceBundleManager = ResourceBundleManager(context, service, useDefaultGuidance = false)
            val date = LocalDate.of(2020, 11, 2)

            val result = resourceBundleManager.getExposureNotificationGuidance(date)
            val paragraph = result[0] as ResourceBundle.Guidance.Element.Paragraph

            val stayAtHomeDate = date.plusDays(5).formatExposureDateShort(context)

            assertEquals("value with a $stayAtHomeDate", paragraph.title)
            assertEquals("value with a $stayAtHomeDate", paragraph.body)
        }

    @Test
    fun `getExposureNotificationGuidance with missing keys falls back to key`() = runBlocking {
        val service = FakeResourceBundleCdnService(
            ResourceBundle(
                mapOf("en" to mapOf("test_key" to "a test value")),
                ResourceBundle.Guidance(
                    5,
                    listOf(ResourceBundle.Guidance.Element.Paragraph("test_key", "missing_key"))
                )
            )
        )

        val resourceBundleManager = ResourceBundleManager(context, service, useDefaultGuidance = false)

        val result = resourceBundleManager.getExposureNotificationGuidance(LocalDate.now())
        val paragraph = result[0] as ResourceBundle.Guidance.Element.Paragraph

        assertEquals("a test value", paragraph.title)
        assertEquals("missing_key", paragraph.body)
    }

    @Test
    @Config(qualifiers = "nl")
    fun `getExposureNotificationGuidance with uses correct locale`() = runBlocking {
        val service = FakeResourceBundleCdnService(
            ResourceBundle(
                mapOf(
                    "en" to mapOf("test_key" to "a test value"),
                    "nl" to mapOf("test_key" to "test waarde")
                ),
                ResourceBundle.Guidance(
                    5,
                    listOf(ResourceBundle.Guidance.Element.Paragraph("test_key", "test_key"))
                )
            )
        )

        val resourceBundleManager = ResourceBundleManager(context, service, useDefaultGuidance = false)

        val result = resourceBundleManager.getExposureNotificationGuidance(LocalDate.now())
        val paragraph = result[0] as ResourceBundle.Guidance.Element.Paragraph

        assertEquals("test waarde", paragraph.title)
        assertEquals("test waarde", paragraph.body)
    }

    @Test
    @Config(qualifiers = "ru") // we don't support russian
    fun `getExposureNotificationGuidance with unsupported app language falls back to en`() =
        runBlocking {
            // even though the bundle contains Russian, the app should fallback to English
            val service = FakeResourceBundleCdnService(
                ResourceBundle(
                    mapOf(
                        "en" to mapOf("test_key" to "a test value"),
                        "ru" to mapOf("test_key" to "russian value")
                    ),
                    ResourceBundle.Guidance(
                        5,
                        listOf(ResourceBundle.Guidance.Element.Paragraph("test_key", "test_key"))
                    )
                )
            )

            val resourceBundleManager = ResourceBundleManager(context, service, useDefaultGuidance = false)

            val result = resourceBundleManager.getExposureNotificationGuidance(LocalDate.now())
            val paragraph = result[0] as ResourceBundle.Guidance.Element.Paragraph

            assertEquals("a test value", paragraph.title)
            assertEquals("a test value", paragraph.body)
        }

    @Test
    @Config(qualifiers = "nl")
    fun `getExposureNotificationGuidance with missing app language falls back to en`() =
        runBlocking {
            // even though the bundle contains Russian, the app should fallback to English
            val service = FakeResourceBundleCdnService(
                ResourceBundle(
                    mapOf("en" to mapOf("test_key" to "a test value")),
                    ResourceBundle.Guidance(
                        5,
                        listOf(ResourceBundle.Guidance.Element.Paragraph("test_key", "test_key"))
                    )
                )
            )

            val resourceBundleManager = ResourceBundleManager(context, service, useDefaultGuidance = false)

            val result = resourceBundleManager.getExposureNotificationGuidance(LocalDate.now())
            val paragraph = result[0] as ResourceBundle.Guidance.Element.Paragraph

            assertEquals("a test value", paragraph.title)
            assertEquals("a test value", paragraph.body)
        }

    @Test
    @Config(qualifiers = "nl")
    fun `getExposureNotificationGuidance with missing key in app language falls back to en value`() =
        runBlocking {
            // even though the bundle contains Russian, the app should fallback to English
            val service = FakeResourceBundleCdnService(
                ResourceBundle(
                    mapOf(
                        "en" to mapOf(
                            "test_key" to "a test value",
                            "second_key" to "second value"
                        ),
                        "nl" to mapOf("test_key" to "test waarde")
                    ),
                    ResourceBundle.Guidance(
                        5,
                        listOf(ResourceBundle.Guidance.Element.Paragraph("test_key", "second_key"))
                    )
                )
            )

            val resourceBundleManager = ResourceBundleManager(context, service, useDefaultGuidance = false)

            val result = resourceBundleManager.getExposureNotificationGuidance(LocalDate.now())
            val paragraph = result[0] as ResourceBundle.Guidance.Element.Paragraph

            assertEquals("test waarde", paragraph.title)
            assertEquals("second value", paragraph.body)
        }

    private class FakeResourceBundleCdnService(val resourceBundle: ResourceBundle) : CdnService {
        override suspend fun getExposureKeySetFile(id: String): Response<ResponseBody> {
            throw IllegalStateException()
        }

        override suspend fun getManifest(cacheStrategy: CacheStrategy?): Manifest {
            return Manifest(emptyList(), "", "", "rb")
        }

        override suspend fun getRiskCalculationParameters(
            id: String,
            cacheStrategy: CacheStrategy?
        ): RiskCalculationParameters {
            throw IllegalStateException()
        }

        override suspend fun getAppConfig(id: String, cacheStrategy: CacheStrategy?): AppConfig {
            throw IllegalStateException()
        }

        override suspend fun getResourceBundle(
            id: String,
            cacheStrategy: CacheStrategy?
        ): ResourceBundle = resourceBundle
    }
}
