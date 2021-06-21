/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest.items

import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemLabTestShareKeysBinding
import nl.rijksoverheid.en.items.BaseBindableItem
import nl.rijksoverheid.en.labtest.LabTestViewModel.UploadResult

class LabTestShareKeysItem(
    private val buttonClickListener: () -> Unit,
    private val uploadResult: UploadResult?,
    private val validPreconditions: Boolean = true
) : BaseBindableItem<ItemLabTestShareKeysBinding>() {

    override fun getLayout() = R.layout.item_lab_test_share_keys

    override fun bind(viewBinding: ItemLabTestShareKeysBinding, position: Int) {
        viewBinding.shareKeysButton.setText(
            if (uploadResult is UploadResult.Success)
                R.string.coronatest_keys_shared
            else
                R.string.coronatest_share_keys
        )
        viewBinding.shareKeysButton.isEnabled =
            validPreconditions && uploadResult !is UploadResult.Success
        viewBinding.shareKeysButton.setOnClickListener { buttonClickListener.invoke() }
    }

    override fun isSameAs(other: Item<*>): Boolean = other is LabTestShareKeysItem
    override fun hasSameContentAs(other: Item<*>) = other is LabTestShareKeysItem &&
        other.uploadResult == uploadResult && other.validPreconditions == validPreconditions
}
