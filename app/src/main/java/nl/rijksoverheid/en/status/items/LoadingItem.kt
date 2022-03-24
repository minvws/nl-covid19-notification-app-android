/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status.items

import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemLoadingBinding
import nl.rijksoverheid.en.items.BaseBindableItem

class LoadingItem : BaseBindableItem<ItemLoadingBinding>() {
    override fun bind(viewBinding: ItemLoadingBinding, position: Int) {
        // nothing to do
    }

    override fun getLayout(): Int = R.layout.item_loading
}
