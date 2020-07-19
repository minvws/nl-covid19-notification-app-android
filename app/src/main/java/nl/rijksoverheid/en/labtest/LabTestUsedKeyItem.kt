/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest

import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemLabTestUsedKeyBinding
import nl.rijksoverheid.en.items.BaseBindableItem

class LabTestUsedKeyItem(private val usedKey: String) :
    BaseBindableItem<ItemLabTestUsedKeyBinding>() {

    override fun getLayout() = R.layout.item_lab_test_used_key

    override fun bind(viewBinding: ItemLabTestUsedKeyBinding, position: Int) {
        viewBinding.text =
            viewBinding.root.context.getString(R.string.lab_test_done_used_key, usedKey)
    }

    override fun isSameAs(other: Item<*>): Boolean =
        other is LabTestUsedKeyItem && other.usedKey == usedKey

    override fun hasSameContentAs(other: Item<*>) =
        other is LabTestUsedKeyItem && other.usedKey == usedKey
}
