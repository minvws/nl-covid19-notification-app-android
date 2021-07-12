/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.items

import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemSpacingBinding

object SpacingItem : BaseBindableItem<ItemSpacingBinding>() {
    override fun getLayout() = R.layout.item_spacing
    override fun bind(viewBinding: ItemSpacingBinding, position: Int) {
        // Nothing to bind
    }

    override fun isSameAs(other: Item<*>): Boolean = other is SpacingItem
    override fun hasSameContentAs(other: Item<*>) = other is SpacingItem
}
