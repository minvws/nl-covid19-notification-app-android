/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import androidx.annotation.StringRes
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemStatusInfoBinding
import nl.rijksoverheid.en.items.BaseBindableItem

class StatusInfoItem(
    @StringRes private val message: Int,
    private val actionMoreInfo: () -> Unit,
    private val actionClose: () -> Unit
) : BaseBindableItem<ItemStatusInfoBinding>() {

    data class ViewState(
        @StringRes val message: Int,
        val actionMoreInfo: () -> Unit,
        val actionClose: () -> Unit,
    )

    override fun bind(viewBinding: ItemStatusInfoBinding, position: Int) {
        viewBinding.viewState = ViewState(
            message, actionMoreInfo, actionClose
        )
    }

    override fun getLayout() = R.layout.item_status_info
}
