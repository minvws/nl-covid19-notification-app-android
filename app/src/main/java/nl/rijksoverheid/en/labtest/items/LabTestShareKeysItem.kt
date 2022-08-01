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
import nl.rijksoverheid.en.databinding.ItemLabTestShareKeysBinding
import nl.rijksoverheid.en.items.BaseBindableItem
import nl.rijksoverheid.en.labtest.LabTestViewModel.KeyState

class LabTestShareKeysItem(
    private val keyState: KeyState,
    uploadKeys: () -> Unit,
    retry: () -> Unit,
    private val hasSharedKeys: Boolean = false,
    private val validPreconditions: Boolean = true
) : BaseBindableItem<ItemLabTestShareKeysBinding>() {
    data class ViewState(
        @StringRes val uploadButtonText: Int,
        val showButton: Boolean,
        val showProgress: Boolean,
        val showError: Boolean,
        val uploadKeysEnabled: Boolean,
        val uploadKeys: () -> Unit,
        val retry: () -> Unit
    )

    private val viewState = ViewState(
        uploadButtonText = if (hasSharedKeys) {
            R.string.coronatest_keys_shared
        } else {
            R.string.coronatest_share_keys
        },
        showButton = keyState is KeyState.Success,
        showProgress = keyState == KeyState.Loading,
        showError = keyState is KeyState.Error,
        uploadKeysEnabled = validPreconditions && !hasSharedKeys,
        uploadKeys = uploadKeys,
        retry = retry
    )

    override fun getLayout() = R.layout.item_lab_test_share_keys

    override fun bind(viewBinding: ItemLabTestShareKeysBinding, position: Int) {
        viewBinding.viewState = viewState
    }

    override fun isSameAs(other: Item<*>): Boolean = other is LabTestShareKeysItem
    override fun hasSameContentAs(other: Item<*>) =
        other is LabTestShareKeysItem &&
            other.keyState == keyState &&
            other.hasSharedKeys == hasSharedKeys &&
            other.validPreconditions == validPreconditions
}
