/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status.items

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.BoolRes
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.GroupieViewHolder
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemStatusHeaderBinding
import nl.rijksoverheid.en.items.BaseBindableItem
import nl.rijksoverheid.en.status.StatusViewModel
import nl.rijksoverheid.en.util.SimpleCountdownTimer
import nl.rijksoverheid.en.util.ext.formatPauseDuration
import nl.rijksoverheid.en.util.formatDaysSince
import nl.rijksoverheid.en.util.formatExposureDate
import java.time.LocalDateTime

class StatusHeaderItem(
    headerState: StatusViewModel.HeaderState,
    primaryAction: () -> Unit,
    secondaryAction: () -> Unit
) : BaseBindableItem<ItemStatusHeaderBinding>() {

    abstract class HeaderViewState(
        @DrawableRes val background: Int,
        @StringRes val iconContentDescription: Int,
        @StringRes val headline: Int,
        @RawRes val animatedIcon: Int = 0,
        @DrawableRes val icon: Int = 0,
        @BoolRes val hideBackgroundOnSmallScreenHeight: Int = R.bool.hide_status_background_on_small_screen_default,
        val showIllustration: Boolean = false,
        @StringRes val enableActionLabel: Int? = null,
        @StringRes val whatsNextActionLabel: Int? = null,
        @StringRes val resetActionLabel: Int? = null,
        val enableAction: () -> Unit = {},
        val whatsNextAction: () -> Unit = {},
        val resetAction: () -> Unit = {},
        val refreshDescriptionUntil: LocalDateTime? = null
    ) {
        abstract fun getDescription(context: Context): String
    }

    // After upgrading AGP lint is flagging some of the fields incorrectly unfortunately
    @SuppressLint("ResourceType")
    private val viewState = when (headerState) {
        StatusViewModel.HeaderState.Active ->
            object : HeaderViewState(
                R.drawable.gradient_status_no_exposure,
                R.string.cd_status_active,
                R.string.status_no_exposure_detected_headline,
                animatedIcon = R.raw.status_active,
                hideBackgroundOnSmallScreenHeight = R.bool.hide_status_active_background_on_small_screen,
                showIllustration = true
            ) {
                override fun getDescription(context: Context) =
                    context.getString(R.string.status_no_exposure_detected_description)
            }
        StatusViewModel.HeaderState.BluetoothDisabled ->
            object : HeaderViewState(
                R.drawable.gradient_status_disabled,
                R.string.cd_status_disabled,
                R.string.status_partly_active_headline,
                animatedIcon = R.raw.status_inactive,
                enableActionLabel = R.string.status_error_bluetooth_action,
                enableAction = primaryAction
            ) {
                override fun getDescription(context: Context) =
                    context.getString(R.string.status_error_bluetooth)
            }
        StatusViewModel.HeaderState.LocationDisabled ->
            object : HeaderViewState(
                R.drawable.gradient_status_disabled,
                R.string.cd_status_disabled,
                R.string.status_partly_active_headline,
                animatedIcon = R.raw.status_inactive,
                enableActionLabel = R.string.status_error_location_action,
                enableAction = primaryAction
            ) {
                override fun getDescription(context: Context) =
                    context.getString(R.string.status_error_location)
            }
        StatusViewModel.HeaderState.Disabled ->
            object : HeaderViewState(
                R.drawable.gradient_status_disabled,
                R.string.cd_status_disabled,
                R.string.status_disabled_headline,
                animatedIcon = R.raw.status_inactive,
                enableActionLabel = R.string.status_en_api_disabled_enable,
                enableAction = primaryAction
            ) {
                override fun getDescription(context: Context) =
                    context.getString(
                        R.string.status_en_api_disabled_description,
                        context.getString(R.string.app_name)
                    )
            }
        StatusViewModel.HeaderState.SyncIssues ->
            object : HeaderViewState(
                R.drawable.gradient_status_disabled,
                R.string.status_partly_active_headline,
                R.string.status_partly_active_headline,
                animatedIcon = R.raw.status_inactive,
                enableActionLabel = R.string.status_error_action_sync_issues,
                enableAction = primaryAction
            ) {
                override fun getDescription(context: Context) =
                    context.getString(R.string.status_error_sync_issues)
            }
        StatusViewModel.HeaderState.SyncIssuesWifiOnly ->
            object : HeaderViewState(
                R.drawable.gradient_status_disabled,
                R.string.status_partly_active_headline,
                R.string.status_partly_active_headline,
                animatedIcon = R.raw.status_inactive,
                enableActionLabel = R.string.status_error_action_disable_battery_optimisation,
                enableAction = primaryAction
            ) {
                override fun getDescription(context: Context) =
                    context.getString(R.string.status_error_sync_issues_wifi_only)
            }
        is StatusViewModel.HeaderState.Paused -> {
            object : HeaderViewState(
                R.drawable.gradient_status_paused,
                R.string.cd_status_paused,
                if (headerState.pausedUntil.isAfter(LocalDateTime.now())) {
                    R.string.status_paused_headline
                } else {
                    R.string.status_paused_duration_reached_headline
                },
                icon = R.drawable.ic_status_paused,
                enableActionLabel = R.string.status_en_api_disabled_enable,
                enableAction = primaryAction,
                refreshDescriptionUntil = headerState.pausedUntil
            ) {
                override fun getDescription(context: Context) =
                    headerState.pausedUntil.formatPauseDuration(context)
            }
        }
        is StatusViewModel.HeaderState.Exposed ->
            object : HeaderViewState(
                R.drawable.gradient_status_exposure,
                R.string.cd_status_exposed,
                R.string.status_exposure_detected_headline,
                animatedIcon = R.raw.status_exposed,
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

    private var refreshTimer: SimpleCountdownTimer? = null
    private var currentViewBinding: ItemStatusHeaderBinding? = null

    override fun bind(viewBinding: ItemStatusHeaderBinding, position: Int) {
        viewBinding.viewState = viewState

        viewState.refreshDescriptionUntil?.let {
            if (viewBinding != currentViewBinding) {
                refreshTimer?.cancel()
                refreshTimer = SimpleCountdownTimer(it) {
                    viewBinding.statusDescription.setHtmlText(
                        viewState.getDescription(viewBinding.statusDescription.context)
                    )
                }
                refreshTimer?.startTimer()
                currentViewBinding = viewBinding
            }
        }
    }

    override fun unbind(viewHolder: GroupieViewHolder<ItemStatusHeaderBinding>) {
        if (viewHolder.binding == currentViewBinding) {
            refreshTimer?.let {
                it.cancel()
                refreshTimer = null
            }
        }
        super.unbind(viewHolder)
    }

    override fun isSameAs(other: Item<*>): Boolean = other is StatusHeaderItem
    override fun hasSameContentAs(other: Item<*>) =
        other is StatusHeaderItem && other.viewState == viewState
}
