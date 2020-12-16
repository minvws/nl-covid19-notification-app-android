/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemStatusFooterBinding
import nl.rijksoverheid.en.items.BaseBindableItem
import nl.rijksoverheid.en.util.formatDateTime
import java.time.LocalDateTime

class StatusFooterItem(private val lastKeysProcessed: LocalDateTime?) : BaseBindableItem<ItemStatusFooterBinding>() {
    override fun getLayout() = R.layout.item_status_footer

    override fun bind(viewBinding: ItemStatusFooterBinding, position: Int) {
        viewBinding.lastKeysSynced.text = if (lastKeysProcessed != null)
            viewBinding.root.context.getString(
                R.string.status_last_keys_processed, lastKeysProcessed.formatDateTime(viewBinding.root.context)
            )
        else
            viewBinding.root.context.getString(R.string.status_no_keys_processed)

        viewBinding.appVersion.text = viewBinding.root.context.getString(
            R.string.status_app_version,
            BuildConfig.VERSION_NAME,
            "${BuildConfig.VERSION_CODE}-${BuildConfig.GIT_VERSION}"
        )
    }
}
