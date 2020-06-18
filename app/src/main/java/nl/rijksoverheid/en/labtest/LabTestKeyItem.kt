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

class LabTestKeyItem(private val keyState: KeyState, retry: () -> Unit) :
    BaseBindableItem<ItemLabTestKeyBinding>() {
    data class ViewState(
        val showProgress: Boolean,
        val showCode: Boolean,
        val showError: Boolean,
        val key: String? = null,
        val retry: () -> Unit
    )

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
    }

    override fun isSameAs(other: Item<*>): Boolean = other is LabTestKeyItem
    override fun hasSameContentAs(other: Item<*>) =
        other is LabTestKeyItem && other.keyState == keyState
}
