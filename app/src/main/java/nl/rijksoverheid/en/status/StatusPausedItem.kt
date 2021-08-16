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
import nl.rijksoverheid.en.util.ext.formatPauseDuration
import java.time.LocalDateTime

class StatusPausedItem(
    val viewState: ViewState
) : BaseBindableItem<ItemStatusPausedBinding>() {

    private var refreshTimer: SimpleCountdownTimer? = null

    private var currentViewBinding: ItemStatusPausedBinding? = null

    data class ViewState(
        val pausedUntil: LocalDateTime,
        val action: () -> Unit
    )

    override fun bind(viewBinding: ItemStatusPausedBinding, position: Int) {
        viewBinding.viewState = viewState
        if (refreshTimer?.countDownTo != viewState.pausedUntil || currentViewBinding != viewBinding) {
            refreshTimer?.cancel()
            refreshTimer = SimpleCountdownTimer(viewState.pausedUntil) {
                viewBinding.infoBoxText.apply {
                    setHtmlText(viewState.pausedUntil.formatPauseDuration(context))
                }
            }
            refreshTimer?.startTimer()
        }
        currentViewBinding = viewBinding
    }

    override fun unbind(viewHolder: GroupieViewHolder<ItemStatusPausedBinding>) {
        if (currentViewBinding == viewHolder.binding) {
            refreshTimer?.let {
                it.cancel()
                refreshTimer = null
            }
            currentViewBinding = null
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
