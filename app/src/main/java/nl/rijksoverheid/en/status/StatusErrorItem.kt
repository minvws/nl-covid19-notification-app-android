/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.status

import android.content.Context
import androidx.annotation.StringRes
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemStatusErrorBinding
import nl.rijksoverheid.en.items.BaseBindableItem

class StatusErrorItem(private val errorState: StatusViewModel.ErrorState) :
    BaseBindableItem<ItemStatusErrorBinding>() {

    abstract class ErrorViewState(
        @StringRes val actionLabel: Int,
        val action: () -> Unit
    ) {
        abstract fun getMessage(context: Context): String
    }

    val viewState = when (errorState) {
        is StatusViewModel.ErrorState.ConsentRequired -> object :
            ErrorViewState(R.string.status_error_action_consent, errorState.action) {
            override fun getMessage(context: Context) = context.getString(
                R.string.status_error_consent_required,
                context.getString(R.string.app_name)
            )
        }
        else -> object :
            ErrorViewState(R.string.status_error_action_sync_issues, errorState.action) {
            override fun getMessage(context: Context) =
                context.getString(R.string.status_error_sync_issues)
        }
    }

    override fun bind(viewBinding: ItemStatusErrorBinding, position: Int) {
        viewBinding.viewState = viewState
    }

    override fun getLayout() = R.layout.item_status_error
}