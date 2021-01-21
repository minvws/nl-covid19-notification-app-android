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

class PauseDurationItem(
    val pauseDurationInHours: Int
) :
    BaseBindableItem<ItemBottomSheetBinding>() {
    override fun getLayout() = R.layout.item_bottom_sheet

    override fun bind(viewBinding: ItemBottomSheetBinding, position: Int) {
        viewBinding.root.context?.let {
            viewBinding.text = it.resources.getQuantityString(R.plurals.pause_duration_hours_option_plurals, pauseDurationInHours, pauseDurationInHours)
        }
    }

    override fun isClickable() = true

    override fun isSameAs(other: Item<*>): Boolean =
        other is PauseDurationItem && other.pauseDurationInHours == pauseDurationInHours

    override fun hasSameContentAs(other: Item<*>) =
        other is PauseDurationItem && other.pauseDurationInHours == pauseDurationInHours
}
