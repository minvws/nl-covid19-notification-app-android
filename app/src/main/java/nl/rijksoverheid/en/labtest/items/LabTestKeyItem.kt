/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest.items

import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemLabTestKeyBinding
import nl.rijksoverheid.en.items.BaseBindableItem
import nl.rijksoverheid.en.labtest.LabTestViewModel.KeyState
import java.util.Locale

class LabTestKeyItem(
    private val keyState: KeyState,
    private val copy: (String) -> Unit,
    private val retry: () -> Unit,
    private val enabled: Boolean = true
) : BaseBindableItem<ItemLabTestKeyBinding>() {
    data class ViewState(
        val showProgress: Boolean,
        val showCode: Boolean,
        val showError: Boolean,
        val key: String? = null,
        val displayKey: String? = null,
        val enabled: Boolean,
        val retry: () -> Unit,
        val keyContentDescription: String?
    )

    override fun getLayout() = R.layout.item_lab_test_key

    override fun bind(viewBinding: ItemLabTestKeyBinding, position: Int) {
        val key = (keyState as? KeyState.Success)?.key
        val viewState = ViewState(
            showProgress = keyState == KeyState.Loading && enabled,
            showCode = keyState is KeyState.Success && enabled,
            showError = keyState is KeyState.Error && enabled,
            key = key,
            displayKey = (keyState as? KeyState.Success)?.displayKey,
            enabled = enabled,
            retry = retry,
            keyContentDescription = if (enabled) {
                key?.lowercase(Locale.ROOT)
                    ?.replace(Regex("(.)"), "$1 ")
                    ?.trimEnd()
            } else {
                viewBinding.root.context.getString(R.string.lab_test_key_cd)
            }
        )

        viewBinding.viewState = viewState
        viewBinding.keyContainer.setOnLongClickListener {
            viewState.key?.let { copy(it) }
            true
        }
    }

    override fun isSameAs(other: Item<*>): Boolean = other is LabTestKeyItem
    override fun hasSameContentAs(other: Item<*>) =
        other is LabTestKeyItem && other.keyState == keyState && other.enabled == enabled
}
