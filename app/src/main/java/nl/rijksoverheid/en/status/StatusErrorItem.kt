/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import android.content.Context
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemStatusErrorBinding
import nl.rijksoverheid.en.util.formatDaysSince
import nl.rijksoverheid.en.util.formatExposureDate

class StatusErrorItem(
    errorState: StatusViewModel.ErrorState,
    action: () -> Unit,
    secondaryAction: () -> Unit = {}
) : BaseStatusErrorItem() {

    override val viewState = when (errorState) {
        is StatusViewModel.ErrorState.ExposureOver14DaysAgo ->
            object :
                ErrorViewState(
                    R.string.status_old_exposure_card_action_delete,
                    action,
                    R.string.status_old_exposure_card_action_more_info,
                    secondaryAction
                ) {
                override fun getMessage(context: Context) = context.getString(
                    R.string.status_old_exposure_card_message,
                    errorState.exposureDate.formatExposureDate(context),
                    errorState.exposureDate.formatDaysSince(context, errorState.clock)
                )
            }
        StatusViewModel.ErrorState.BluetoothDisabled ->
            object :
                ErrorViewState(R.string.status_error_bluetooth_action, action) {
                override fun getMessage(context: Context) =
                    context.getString(R.string.status_error_bluetooth_card)
            }
        StatusViewModel.ErrorState.LocationDisabled ->
            object :
                ErrorViewState(R.string.status_error_location_action, action) {
                override fun getMessage(context: Context) =
                    context.getString(R.string.status_error_location_card)
            }
        StatusViewModel.ErrorState.ConsentRequired ->
            object :
                ErrorViewState(R.string.status_error_action_consent, action) {
                override fun getMessage(context: Context) = context.getString(
                    R.string.status_error_consent_required,
                    context.getString(R.string.app_name)
                )
            }
        StatusViewModel.ErrorState.SyncIssuesWifiOnly ->
            object :
                ErrorViewState(R.string.status_error_action_disable_battery_optimisation, action) {
                override fun getMessage(context: Context) =
                    context.getString(R.string.sync_issue_notification_message)
            }
        StatusViewModel.ErrorState.SyncIssues ->
            object :
                ErrorViewState(R.string.status_error_action_sync_issues, action) {
                override fun getMessage(context: Context) =
                    context.getString(R.string.status_error_sync_issues)
            }
        StatusViewModel.ErrorState.NotificationsDisabled ->
            object :
                ErrorViewState(R.string.status_error_action_notifications_disabled, action) {
                override fun getMessage(context: Context) =
                    context.getString(R.string.status_error_notifications_disabled)
            }
        StatusViewModel.ErrorState.None -> null
    }

    override fun bind(viewBinding: ItemStatusErrorBinding, position: Int) {
        viewBinding.viewState = viewState
    }

    override fun getLayout() = R.layout.item_status_error
}
