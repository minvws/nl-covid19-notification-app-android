/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.items.ActionItem

sealed class KeyShareOptionItem(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes subtitle: Int
) : ActionItem(icon, title, subtitle) {

    object CoronaTest : KeyShareOptionItem(
        R.drawable.ic_coronatest,
        R.string.post_keys_options_coronatest_title,
        R.string.post_keys_options_coronatest_subtitle
    ) {
        override fun isSameAs(other: Item<*>) = other is CoronaTest
    }

    object GGD : KeyShareOptionItem(
        R.drawable.ic_phone,
        R.string.post_keys_options_ggd_title,
        R.string.post_keys_options_ggd_subtitle
    ) {
        override fun isSameAs(other: Item<*>): Boolean = other is GGD
    }
}
