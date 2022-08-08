/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.resource

import android.content.Context
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.CacheStrategy
import nl.rijksoverheid.en.api.CdnService
import nl.rijksoverheid.en.api.model.ResourceBundle
import nl.rijksoverheid.en.util.formatDaysSince
import nl.rijksoverheid.en.util.formatExposureDate
import nl.rijksoverheid.en.util.formatExposureDateShort
import timber.log.Timber
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val DEFAULT_LANGUAGE = "en"

/**
 * Manager class for fetching the resource bundle and getting correct guidance elements.
 */
class ResourceBundleManager(
    private val context: Context,
    private val cdnService: CdnService,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val useDefaultGuidance: Boolean
) {

    private suspend fun loadResourceBundle(
        refreshFromServer: Boolean = false
    ): ResourceBundle {
        return if (useDefaultGuidance) {
            loadDefaultResourceBundle()
        } else {
            getResourceBundleFromCacheOrNetwork(refreshFromServer) ?: loadDefaultResourceBundle()
        }
    }

    private fun loadDefaultResourceBundle(): ResourceBundle {
        return context.resources.openRawResource(R.raw.default_guidance).use {
            ResourceBundle.load(it) ?: error("Error loading default guidance")
        }
    }

    private suspend fun getResourceBundleFromCacheOrNetwork(refreshFromServer: Boolean): ResourceBundle? {
        return try {
            cdnService.getManifest(CacheStrategy.CACHE_FIRST).resourceBundleId?.let {
                cdnService.getResourceBundle(
                    it,
                    if (refreshFromServer) CacheStrategy.CACHE_LAST else CacheStrategy.CACHE_FIRST
                )
            }
        } catch (ex: Exception) {
            Timber.w(ex, "Error fetching resource bundle")
            null
        }
    }

    suspend fun getExposureNotificationGuidance(exposureDate: LocalDate, notificationReceiveDate: LocalDate?): List<ResourceBundle.Guidance.Element> {
        val bundle = loadResourceBundle()
        val language = context.getString(R.string.app_language)
        val localeMap = bundle.resources[language]
        val fallback = bundle.resources[DEFAULT_LANGUAGE]
            ?: error("No resources for default language $DEFAULT_LANGUAGE")

        val exposureDays = ChronoUnit.DAYS.between(exposureDate, LocalDate.now(clock)).toInt()
        val layout = bundle.guidance.layoutByRelativeExposureDay.find {
            val min = it.exposureDaysLowerBoundary
            val max = it.exposureDaysUpperBoundary
            notificationReceiveDate != null && (min == null || exposureDays >= min) && (max == null || exposureDays <= max)
        }?.layout ?: bundle.guidance.layout

        return layout.mapNotNull {
            when (it) {
                is ResourceBundle.Guidance.Element.Paragraph -> {
                    val title = localeMap?.get(it.title) ?: fallback[it.title] ?: it.title
                    val body = localeMap?.get(it.body) ?: fallback[it.body] ?: it.body
                    ResourceBundle.Guidance.Element.Paragraph(
                        title.replacePlaceHolders(exposureDate, notificationReceiveDate),
                        body.replacePlaceHolders(exposureDate, notificationReceiveDate)
                    )
                }
                is ResourceBundle.Guidance.Element.Unknown -> null
            }
        }
    }

    suspend fun getAppMessageResources(title: String, message: String): Pair<String, String> {
        val bundle = loadResourceBundle()
        val language = context.getString(R.string.app_language)
        val localeMap = bundle.resources[language]
        val fallback = bundle.resources[DEFAULT_LANGUAGE]
            ?: error("No resources for default language $DEFAULT_LANGUAGE")

        return Pair(
            localeMap?.get(title) ?: fallback[title] ?: title,
            localeMap?.get(message) ?: fallback[message] ?: message
        )
    }

    suspend fun getEndOfLifeResources(titleRef: String?, bodyRef: String?): Pair<String, String> {
        // Refresh the content from server because the processManifest job has probably been deactivated during end of life
        val bundle = loadResourceBundle(true)
        val language = context.getString(R.string.app_language)
        val localeMap = bundle.resources[language]
        val fallback = bundle.resources[DEFAULT_LANGUAGE]
            ?: error("No resources for default language $DEFAULT_LANGUAGE")

        val title = titleRef?.let { localeMap?.get(it) ?: fallback[it] } ?: context.getString(R.string.end_of_life_headline)
        val body = bodyRef?.let { localeMap?.get(it) ?: fallback[it] } ?: context.getString(R.string.end_of_life_description)

        return Pair(title, body)
    }

    private fun String.replacePlaceHolders(exposureDate: LocalDate, notificationReceiveDate: LocalDate?): String {
        val daysSinceExposure = exposureDate.formatDaysSince(context, clock)
        return this.replace("\\\n", "\n")
            .replaceExposureDateWithOffset(exposureDate, "ExposureDate") {
                it.formatExposureDate(context)
            }
            .replaceExposureDateWithOffset(exposureDate, "ExposureDateShort") {
                it.formatExposureDateShort(context)
            }
            .replace("{ExposureDaysAgo}", daysSinceExposure)
            .replace("{NotificationReceivedDate}", notificationReceiveDate?.formatExposureDate(context) ?: "")
    }

    private fun String.replaceExposureDateWithOffset(
        exposureDate: LocalDate,
        token: String,
        block: (LocalDate) -> String
    ): String {
        @Suppress("RegExpRedundantEscape") // crashes on device when not escaping the last '}'
        val regex = Regex(pattern = "\\{$token(\\+([0-9]+))?\\}")
        return regex.replace(this) {
            val days =
                if (it.groupValues.size >= 2 && it.groupValues[1].isNotEmpty()) it.groupValues[1].toLong() else 0
            block(exposureDate.plusDays(days))
        }
    }
}
