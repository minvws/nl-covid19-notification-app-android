/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import com.xwray.groupie.viewbinding.GroupieViewHolder
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemStatusPausedBinding
import nl.rijksoverheid.en.items.BaseBindableItem
import nl.rijksoverheid.en.util.SimpleCountdownTimer
import nl.rijksoverheid.en.util.formatPauseDuration
import java.time.LocalDateTime

class StatusPausedItem(
    val viewState: ViewState
) : BaseBindableItem<ItemStatusPausedBinding>() {

    private var refreshTimer: SimpleCountdownTimer? = null

    data class ViewState(
        val pausedUntil: LocalDateTime,
        val action: () -> Unit
    )

    override fun bind(viewBinding: ItemStatusPausedBinding, position: Int) {
        viewBinding.viewState = viewState
        if (refreshTimer?.countDownTo != viewState.pausedUntil) {
            refreshTimer?.cancel()
            refreshTimer = SimpleCountdownTimer(viewState.pausedUntil) {
                viewBinding.infoBoxText.apply {
                    text = viewState.pausedUntil.formatPauseDuration(context)
                }
            }
            refreshTimer?.startTimer()
        }
    }

    override fun unbind(viewHolder: GroupieViewHolder<ItemStatusPausedBinding>) {
        refreshTimer?.let {
            it.cancel()
            refreshTimer = null
        }
        super.unbind(viewHolder)
    }

    override fun getLayout() = R.layout.item_status_paused

    companion object {
        fun forStatus(
            state: StatusViewModel.NotificationState.Paused,
            onAction: (StatusViewModel.NotificationState, StatusSection.NotificationAction) -> Unit
        ): StatusPausedItem =
            StatusPausedItem(
                ViewState(state.pausedUntil) {
                    onAction(
                        state,
                        StatusSection.NotificationAction.Primary
                    )
                }
            )
    }
}
