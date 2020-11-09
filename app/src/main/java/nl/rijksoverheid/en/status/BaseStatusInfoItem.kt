/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import android.content.Context
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemStatusInfoBinding
import nl.rijksoverheid.en.items.BaseBindableItem

abstract class BaseStatusInfoItem : BaseBindableItem<ItemStatusInfoBinding>() {

    abstract class InfoViewState(
        val actionMoreInfo: () -> Unit,
        val actionClose: () -> Unit,
    ) {
        abstract fun getMessage(context: Context): String
    }

    abstract val viewState: InfoViewState

    override fun bind(viewBinding: ItemStatusInfoBinding, position: Int) {
        viewBinding.viewState = viewState
    }

    override fun getLayout() = R.layout.item_status_info
}
