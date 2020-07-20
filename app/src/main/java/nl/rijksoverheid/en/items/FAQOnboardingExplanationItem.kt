/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.items

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.xwray.groupie.Item
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemFaqOnboardingExplanationBinding

class FAQOnboardingExplanationItem(
    @StringRes headline: Int,
    @StringRes description: Int,
    @DrawableRes illustration: Int,
    isExample: Boolean = false
) : BaseBindableItem<ItemFaqOnboardingExplanationBinding>() {
    data class ViewState(
        @StringRes val headline: Int,
        @StringRes val description: Int,
        @DrawableRes val illustration: Int,
        val isExample: Boolean
    )

    val viewState = ViewState(headline, description, illustration, isExample)

    override fun getLayout() = R.layout.item_faq_onboarding_explanation
    override fun bind(viewBinding: ItemFaqOnboardingExplanationBinding, position: Int) {
        viewBinding.viewState = viewState
    }

    override fun isSameAs(other: Item<*>): Boolean =
        other is FAQOnboardingExplanationItem && other.viewState == viewState

    override fun hasSameContentAs(other: Item<*>) =
        other is FAQOnboardingExplanationItem && other.viewState == viewState
}
