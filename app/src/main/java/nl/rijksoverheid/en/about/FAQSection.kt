/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.about

import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.xwray.groupie.Section
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.about.FAQItemId.ANONYMOUS
import nl.rijksoverheid.en.about.FAQItemId.BLUETOOTH
import nl.rijksoverheid.en.about.FAQItemId.DELETION
import nl.rijksoverheid.en.about.FAQItemId.INTEROPERABILITY
import nl.rijksoverheid.en.about.FAQItemId.INTEROP_COUNTRIES
import nl.rijksoverheid.en.about.FAQItemId.LOCATION
import nl.rijksoverheid.en.about.FAQItemId.LOCATION_PERMISSION
import nl.rijksoverheid.en.about.FAQItemId.NOTIFICATION
import nl.rijksoverheid.en.about.FAQItemId.NOTIFICATION_MESSAGE
import nl.rijksoverheid.en.about.FAQItemId.ONBOARDING
import nl.rijksoverheid.en.about.FAQItemId.PAUSE
import nl.rijksoverheid.en.about.FAQItemId.POWER_USAGE
import nl.rijksoverheid.en.about.FAQItemId.REASON
import nl.rijksoverheid.en.about.FAQItemId.TECHNICAL
import nl.rijksoverheid.en.about.FAQItemId.UPLOAD_KEYS
import nl.rijksoverheid.en.items.ButtonItem
import nl.rijksoverheid.en.items.FAQOnboardingExplanationItem
import nl.rijksoverheid.en.items.HeaderItem
import nl.rijksoverheid.en.items.ParagraphItem
import nl.rijksoverheid.en.notification.GenericNotificationSection

@Keep
enum class FAQItemId(@StringRes val label: Int) {
    REASON(R.string.faq_reason),
    LOCATION(R.string.faq_location),
    ANONYMOUS(R.string.faq_anonymous),
    NOTIFICATION(R.string.faq_notification),
    NOTIFICATION_MESSAGE(R.string.faq_notification_message),
    UPLOAD_KEYS(R.string.faq_upload_keys),
    BLUETOOTH(R.string.faq_bluetooth),
    POWER_USAGE(R.string.faq_power_usage),
    PAUSE(R.string.faq_pause),
    INTEROPERABILITY(R.string.faq_interoperability),
    DELETION(R.string.faq_deletion),
    LOCATION_PERMISSION(R.string.faq_location_permission),
    TECHNICAL(R.string.faq_technical),
    ONBOARDING(R.string.about_onboarding_title),
    INTEROP_COUNTRIES(R.string.faq_interop_countries)
}

class FAQDetailSections(
    private val openAndroidSettings: () -> Unit = {},
    private val openAppSettings: () -> Unit = {}
) {
    fun getSection(faqItemId: FAQItemId) = when (faqItemId) {
        REASON -> Section(
            listOf(
                HeaderItem(R.string.faq_reason),
                ParagraphItem(R.string.faq_reason_paragraph_1),
                ParagraphItem(R.string.faq_reason_paragraph_2),
                ParagraphItem(R.string.faq_reason_paragraph_3),
                ParagraphItem(R.string.faq_reason_paragraph_4)
            )
        )
        LOCATION -> Section(
            listOf(
                HeaderItem(R.string.faq_location),
                ParagraphItem(R.string.faq_location_paragraph_1)
            )
        )
        ANONYMOUS -> Section(
            listOf(
                HeaderItem(R.string.faq_anonymous),
                ParagraphItem(R.string.faq_anonymous_paragraph_1),
                ParagraphItem(R.string.faq_anonymous_paragraph_2)
            )
        )
        NOTIFICATION -> Section(
            listOf(
                HeaderItem(R.string.faq_notification),
                ParagraphItem(R.string.faq_notification_paragraph_1)
            )
        )
        NOTIFICATION_MESSAGE -> GenericNotificationSection()
        UPLOAD_KEYS -> Section(
            listOf(
                HeaderItem(R.string.faq_upload_keys),
                ParagraphItem(R.string.faq_upload_keys_paragraph_1),
                ParagraphItem(R.string.faq_upload_keys_paragraph_2)
            )
        )
        BLUETOOTH -> Section(
            listOf(
                HeaderItem(R.string.faq_bluetooth),
                ParagraphItem(R.string.faq_bluetooth_paragraph_1)
            )
        )
        POWER_USAGE -> Section(
            listOf(
                HeaderItem(R.string.faq_power_usage),
                ParagraphItem(R.string.faq_power_usage_paragraph_1)
            )
        )
        PAUSE -> Section(
            listOf(
                HeaderItem(R.string.faq_pause),
                ParagraphItem(R.string.faq_pause_paragraph_1),
                ParagraphItem(R.string.faq_pause_paragraph_2),
                ParagraphItem(R.string.faq_pause_paragraph_3),
                ButtonItem(R.string.faq_app_settings_button, openAppSettings)
            )
        )
        INTEROPERABILITY -> Section(
            listOf(
                HeaderItem(R.string.faq_interoperability),
                ParagraphItem(R.string.faq_interoperability_paragraph_1),
                ParagraphItem(R.string.faq_interoperability_paragraph_2),
                ParagraphItem(R.string.faq_interoperability_paragraph_3)
            )
        )
        DELETION -> Section(
            listOf(
                HeaderItem(R.string.faq_deletion),
                ParagraphItem(R.string.faq_deletion_paragraph_1),
                ButtonItem(R.string.faq_app_settings_button, openAndroidSettings)
            )
        )
        LOCATION_PERMISSION -> Section(
            listOf(
                HeaderItem(R.string.faq_location_permission),
                ParagraphItem(R.string.faq_location_permission_paragraph_1)
            )
        )
        TECHNICAL -> Section(
            listOf(
                CenteredIllustrationItem(R.drawable.illustration_technical_1),
                HeaderItem(R.string.faq_technical_header_1),
                ParagraphItem(R.string.faq_technical_paragraph_2),
                CenteredIllustrationItem(R.drawable.illustration_technical_2),
                HeaderItem(R.string.faq_technical_header_3),
                ParagraphItem(R.string.faq_technical_paragraph_4),
                CenteredIllustrationItem(R.drawable.illustration_technical_3),
                HeaderItem(R.string.faq_technical_header_5),
                ParagraphItem(R.string.faq_technical_paragraph_6),
                CenteredIllustrationItem(R.drawable.illustration_technical_4),
                HeaderItem(R.string.faq_technical_header_7),
                ParagraphItem(R.string.faq_technical_paragraph_8),
                CenteredIllustrationItem(R.drawable.illustration_technical_5),
                HeaderItem(R.string.faq_technical_header_9),
                ParagraphItem(R.string.faq_technical_paragraph_10),
                FAQOnboardingItem(),
                GithubItem()
            )
        )
        ONBOARDING -> Section(
            listOf(
                FAQOnboardingExplanationItem(
                    R.string.onboarding_explanation_1_headline,
                    R.string.onboarding_explanation_1_description,
                    R.drawable.illustration_explanation_step_1
                ),
                FAQOnboardingExplanationItem(
                    R.string.onboarding_explanation_2_headline,
                    R.string.onboarding_explanation_2_description,
                    R.drawable.illustration_explanation_step_2
                ),
                FAQOnboardingExplanationItem(
                    R.string.onboarding_explanation_3_headline,
                    R.string.onboarding_explanation_3_description,
                    R.drawable.illustration_explanation_step_3
                ),
                FAQOnboardingExplanationItem(
                    R.string.onboarding_example_1_headline,
                    R.string.onboarding_example_1_description,
                    R.drawable.illustration_explanation_example_1,
                    isExample = true
                ),
                FAQOnboardingExplanationItem(
                    R.string.onboarding_example_2_headline,
                    R.string.onboarding_example_2_description,
                    R.drawable.illustration_explanation_example_2,
                    isExample = true
                ),
                FAQTechnicalExplanationItem()
            )
        )
        INTEROP_COUNTRIES ->
            throw IllegalArgumentException("INTEROP_COUNTRIES should be opened as an external web page.")
    }
}
