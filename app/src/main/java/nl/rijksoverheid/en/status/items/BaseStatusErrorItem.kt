/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status.items

import android.content.Context
import androidx.annotation.StringRes
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemStatusErrorBinding
import nl.rijksoverheid.en.items.BaseBindableItem

abstract class BaseStatusErrorItem : BaseBindableItem<ItemStatusErrorBinding>() {

    abstract class ErrorViewState(
        @StringRes val actionLabel: Int,
        val action: () -> Unit
    ) {
        abstract fun getMessage(context: Context): String
        abstract fun getTitle(context: Context): String
    }

    abstract val viewState: ErrorViewState?

    override fun bind(viewBinding: ItemStatusErrorBinding, position: Int) {
        viewBinding.viewState = viewState
    }

    override fun getLayout() = R.layout.item_status_error
}
