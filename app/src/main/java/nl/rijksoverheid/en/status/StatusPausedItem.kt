/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.status

import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemStatusPausedBinding
import nl.rijksoverheid.en.items.BaseBindableItem
import nl.rijksoverheid.en.settings.Settings

class StatusPausedItem(
    val viewState: ViewState
) : BaseBindableItem<ItemStatusPausedBinding>() {

    data class ViewState(
        val pausedState: Settings.PausedState,
        val durationHours: Long,
        val durationMinutes: Long,
        val action: () -> Unit
    )

    override fun bind(viewBinding: ItemStatusPausedBinding, position: Int) {
        viewBinding.viewState = viewState
    }

    override fun getLayout() = R.layout.item_status_paused
}