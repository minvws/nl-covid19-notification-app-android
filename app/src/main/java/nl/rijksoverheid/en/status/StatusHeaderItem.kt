/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import android.content.Context
import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemStatusHeaderBinding
import nl.rijksoverheid.en.items.BaseBindableItem
import nl.rijksoverheid.en.util.formatDaysSince
import nl.rijksoverheid.en.util.formatExposureDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class StatusHeaderItem(
    headerState: StatusViewModel.HeaderState,
    primaryAction: () -> Unit,
    secondaryAction: () -> Unit
) : BaseBindableItem<ItemStatusHeaderBinding>() {

    abstract class HeaderViewState(
        @DrawableRes val background: Int,
        @RawRes val icon: Int,
        @StringRes val iconContentDescription: Int,
        @StringRes val headline: Int,
        @DrawableRes val illustration: Int? = null,
        val showIllustration: Boolean = false,
        @StringRes val enableActionLabel: Int? = null,
        @StringRes val whatsNextActionLabel: Int? = null,
        @StringRes val resetActionLabel: Int? = null,
        val enableAction: () -> Unit = {},
        val whatsNextAction: () -> Unit = {},
        val resetAction: () -> Unit = {}
    ) {
        abstract fun getDescription(context: Context): CharSequence
    }

    private val viewState = when (headerState) {
        StatusViewModel.HeaderState.Active ->
            object : HeaderViewState(
                R.drawable.gradient_status_no_exposure,
                R.raw.status_active,
                R.string.cd_status_active,
                R.string.status_no_exposure_detected_headline,
                showIllustration = true
            ) {
                override fun getDescription(context: Context) =
                    context.getString(R.string.status_no_exposure_detected_description)
            }
        is StatusViewModel.HeaderState.Paused ->
            object : HeaderViewState(
                R.drawable.gradient_status_paused,
                R.raw.status_inactive,
                R.string.cd_status_paused,
                R.string.status_paused_headline,
                enableActionLabel = R.string.status_en_api_disabled_enable,
                enableAction = primaryAction
            ) {
                override fun getDescription(context: Context): CharSequence {
                    return if (headerState.pausedUntil.isAfter(LocalDateTime.now())) {
                        val format = if (DateFormat.is24HourFormat(context)) "HH:mm" else "hh:mm a"
                        HtmlCompat.fromHtml(
                            context.getString(
                                R.string.status_en_api_paused_description,
                                DateTimeFormatter.ofPattern(format, Locale.getDefault())
                                    .format(headerState.pausedUntil)
                            ),
                            HtmlCompat.FROM_HTML_MODE_COMPACT
                        )
                    } else {
                        context.getString(R.string.status_en_api_paused_duration_reached)
                    }
                }
            }
        is StatusViewModel.HeaderState.Disabled ->
            object : HeaderViewState(
                R.drawable.gradient_status_disabled,
                R.raw.status_inactive,
                R.string.cd_status_disabled,
                R.string.status_disabled_headline,
                enableActionLabel = R.string.status_en_api_disabled_enable,
                enableAction = primaryAction
            ) {
                override fun getDescription(context: Context) =
                    context.getString(R.string.status_en_api_disabled_description, context.getString(R.string.app_name))
            }
        is StatusViewModel.HeaderState.SyncIssues ->
            object : HeaderViewState(
                R.drawable.gradient_status_disabled,
                R.raw.status_inactive,
                R.string.cd_status_disabled,
                R.string.status_disabled_headline,
                enableActionLabel = R.string.status_error_action_sync_issues,
                enableAction = primaryAction
            ) {
                override fun getDescription(context: Context) = context.getString(R.string.status_error_sync_issues)
            }
        is StatusViewModel.HeaderState.Exposed ->
            object : HeaderViewState(
                R.drawable.gradient_status_exposure,
                R.raw.status_exposed,
                R.string.cd_status_exposed,
                R.string.status_exposure_detected_headline,
                whatsNextActionLabel = R.string.status_exposure_what_next,
                resetActionLabel = R.string.status_reset_exposure,
                whatsNextAction = primaryAction,
                resetAction = secondaryAction
            ) {
                override fun getDescription(context: Context) = context.getString(
                    R.string.status_exposure_detected_description,
                    headerState.date.formatExposureDate(context),
                    headerState.date.formatDaysSince(context, headerState.clock)
                )
            }
    }

    override fun getLayout() = R.layout.item_status_header

    override fun bind(viewBinding: ItemStatusHeaderBinding, position: Int) {
        viewBinding.viewState = viewState
    }

    override fun isSameAs(other: Item<*>): Boolean = other is StatusHeaderItem
    override fun hasSameContentAs(other: Item<*>) =
        other is StatusHeaderItem && other.viewState == viewState
}
