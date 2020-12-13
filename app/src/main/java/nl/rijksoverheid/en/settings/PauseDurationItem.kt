/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.settings

import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemBottomSheetBinding
import nl.rijksoverheid.en.items.BaseBindableItem
import java.time.LocalTime

sealed class PauseDuration {
    data class Hours(val amountOfHours: Int) : PauseDuration()
    data class Until(val time: LocalTime) : PauseDuration()
}

class PauseDurationItem(
    val pauseDuration: PauseDuration
) :
    BaseBindableItem<ItemBottomSheetBinding>() {
    override fun getLayout() = R.layout.item_bottom_sheet

    override fun bind(viewBinding: ItemBottomSheetBinding, position: Int) {
        viewBinding.root.context?.let {
            viewBinding.text = when (pauseDuration) {
                is PauseDuration.Hours -> it.getString(R.string.pause_duration_hours_option, pauseDuration.amountOfHours)
                is PauseDuration.Until -> it.getString(R.string.pause_duration_until_option, pauseDuration.time.toString())
            }
        }
    }

    override fun isClickable() = true
    override fun isSameAs(other: Item<*>): Boolean = other == pauseDuration
    override fun hasSameContentAs(other: Item<*>) = other == pauseDuration
}
