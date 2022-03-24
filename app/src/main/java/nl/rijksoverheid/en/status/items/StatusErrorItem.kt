/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
package nl.rijksoverheid.en.status.items

import android.content.Context
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemStatusErrorBinding
import nl.rijksoverheid.en.status.StatusViewModel

class StatusErrorItem(
    error: StatusViewModel.NotificationState.Error,
    private val action: () -> Unit
) : BaseStatusErrorItem() {

    override val viewState = when (error) {
        StatusViewModel.NotificationState.Error.BluetoothDisabled ->
            object :
                ErrorViewState(R.string.status_error_bluetooth_action, action) {
                override fun getMessage(context: Context) = context.getString(R.string.status_error_bluetooth_card)
                override fun getTitle(context: Context) = context.getString(R.string.status_partly_active_headline)
            }
        StatusViewModel.NotificationState.Error.LocationDisabled ->
            object :
                ErrorViewState(R.string.status_error_location_action, action) {
                override fun getMessage(context: Context) = context.getString(R.string.status_error_location_card)
                override fun getTitle(context: Context) = context.getString(R.string.status_partly_active_headline)
            }
        StatusViewModel.NotificationState.Error.ConsentRequired ->
            object :
                ErrorViewState(R.string.status_error_action_consent, action) {
                override fun getMessage(context: Context) = context.getString(
                    R.string.status_error_consent_required,
                    context.getString(R.string.app_name)
                )
                override fun getTitle(context: Context) = context.getString(R.string.status_disabled_headline)
            }
        StatusViewModel.NotificationState.Error.SyncIssuesWifiOnly ->
            object :
                ErrorViewState(R.string.status_error_action_disable_battery_optimisation, action) {
                override fun getMessage(context: Context) =
                    context.getString(R.string.sync_issue_notification_message)
                override fun getTitle(context: Context) = context.getString(R.string.status_partly_active_headline)
            }
        StatusViewModel.NotificationState.Error.SyncIssues ->
            object :
                ErrorViewState(R.string.status_error_action_sync_issues, action) {
                override fun getMessage(context: Context) =
                    context.getString(R.string.status_error_sync_issues)
                override fun getTitle(context: Context) = context.getString(R.string.status_partly_active_headline)
            }
        StatusViewModel.NotificationState.Error.NotificationsDisabled ->
            object :
                ErrorViewState(R.string.status_error_action_notifications_disabled, action) {
                override fun getMessage(context: Context) =
                    context.getString(R.string.status_error_notifications_disabled)

                override fun getTitle(context: Context) = context.getString(R.string.status_error_notifications_disabled_headline)
            }
    }

    override fun bind(viewBinding: ItemStatusErrorBinding, position: Int) {
        viewBinding.viewState = viewState
    }

    override fun getLayout() = R.layout.item_status_error
}
