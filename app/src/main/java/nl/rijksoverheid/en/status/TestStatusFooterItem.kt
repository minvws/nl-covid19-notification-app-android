/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import com.xwray.groupie.Item
import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemStatusFooterTestBinding
import nl.rijksoverheid.en.items.BaseBindableItem

class TestStatusFooterItem : BaseBindableItem<ItemStatusFooterTestBinding>() {
    override fun getLayout() = R.layout.item_status_footer_test

    override fun bind(viewBinding: ItemStatusFooterTestBinding, position: Int) {
        viewBinding.footer.text = viewBinding.root.context.getString(
            R.string.footer_test_phase,
            BuildConfig.VERSION_NAME,
            "${BuildConfig.VERSION_CODE}-${BuildConfig.GIT_VERSION}"
        )
    }

    override fun isClickable() = true
    override fun isSameAs(other: Item<*>) = other is TestStatusFooterItem
    override fun hasSameContentAs(other: Item<*>) = other is TestStatusFooterItem
}
