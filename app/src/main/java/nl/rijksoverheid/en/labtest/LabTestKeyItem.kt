/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest

import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemLabTestKeyBinding
import nl.rijksoverheid.en.items.BaseBindableItem
import nl.rijksoverheid.en.labtest.LabTestViewModel.KeyState
import java.util.Locale

class LabTestKeyItem(private val keyState: KeyState, private val copy: (String) -> Unit, retry: () -> Unit) :
    BaseBindableItem<ItemLabTestKeyBinding>() {
    data class ViewState(
        val showProgress: Boolean,
        val showCode: Boolean,
        val showError: Boolean,
        val key: String? = null,
        val retry: () -> Unit
    ) {
        val keyPart1 = key?.substring(0..2)
        val keyPart2 = key?.substring(3..4)
        val keyPart3 = key?.substring(5..6)
        val keyContentDescription = key?.toLowerCase(Locale.ROOT)?.replace("-", "")
    }

    private val viewState = ViewState(
        showProgress = keyState == KeyState.Loading,
        showCode = keyState is KeyState.Success,
        showError = keyState is KeyState.Error,
        key = (keyState as? KeyState.Success)?.key,
        retry = retry
    )

    override fun getLayout() = R.layout.item_lab_test_key

    override fun bind(viewBinding: ItemLabTestKeyBinding, position: Int) {
        viewBinding.viewState = viewState
        viewBinding.keyContainer.setOnLongClickListener {
            viewState.key?.let { copy(it) }
            true
        }
    }

    override fun isSameAs(other: Item<*>): Boolean = other is LabTestKeyItem
    override fun hasSameContentAs(other: Item<*>) =
        other is LabTestKeyItem && other.keyState == keyState
}
