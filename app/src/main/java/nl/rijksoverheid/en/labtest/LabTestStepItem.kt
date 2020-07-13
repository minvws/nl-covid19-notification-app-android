/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest

import androidx.annotation.StringRes
import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemLabTestStepBinding
import nl.rijksoverheid.en.items.BaseBindableItem

class LabTestStepItem(@StringRes private val text: Int, private val counter: Int) :
    BaseBindableItem<ItemLabTestStepBinding>() {
    override fun getLayout() = R.layout.item_lab_test_step

    override fun bind(viewBinding: ItemLabTestStepBinding, position: Int) {
        viewBinding.text = text
        viewBinding.counter = counter.toString()
    }

    override fun isSameAs(other: Item<*>): Boolean = other is LabTestStepItem && other.text == text
    override fun hasSameContentAs(other: Item<*>) = other is LabTestStepItem && other.text == text
}
