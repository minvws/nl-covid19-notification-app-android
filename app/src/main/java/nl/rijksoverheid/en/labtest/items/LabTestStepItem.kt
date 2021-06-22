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
import nl.rijksoverheid.en.databinding.ItemLabTestStepBinding
import nl.rijksoverheid.en.items.BaseBindableItem

class LabTestStepItem(
    @StringRes private val text: Int,
    private val counter: Int,
    private val isFirstElement: Boolean = false,
    private val isLastElement: Boolean = false,
    private val enabled: Boolean = true
) : BaseBindableItem<ItemLabTestStepBinding>() {

    data class ViewState(
        val counter: String,
        val text: Int,
        val showTopLine: Boolean,
        val showBottomLine: Boolean,
        val enabled: Boolean
    )

    override fun getLayout() = R.layout.item_lab_test_step

    override fun bind(viewBinding: ItemLabTestStepBinding, position: Int) {
        viewBinding.viewState = ViewState(counter.toString(), text, !isFirstElement, !isLastElement, enabled)
    }

    override fun isSameAs(other: Item<*>): Boolean = other is LabTestStepItem && other.text == text
    override fun hasSameContentAs(other: Item<*>) =
        other is LabTestStepItem && other.text == text && other.enabled == enabled
}
