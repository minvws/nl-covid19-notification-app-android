/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest.items

import androidx.annotation.StringRes
import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemLabTestStepDescriptionBinding
import nl.rijksoverheid.en.items.BaseBindableItem

class LabTestStepDescriptionItem(
    @StringRes private val text: Int,
    private val enabled: Boolean = true
) : BaseBindableItem<ItemLabTestStepDescriptionBinding>() {

    override fun getLayout() = R.layout.item_lab_test_step_description

    override fun bind(viewBinding: ItemLabTestStepDescriptionBinding, position: Int) {
        viewBinding.description.setText(text)
        viewBinding.description.isEnabled = enabled
    }

    override fun isSameAs(other: Item<*>): Boolean =
        other is LabTestStepDescriptionItem && other.text == text
    override fun hasSameContentAs(other: Item<*>) =
        other is LabTestStepDescriptionItem && other.text == text && other.enabled == enabled
}
