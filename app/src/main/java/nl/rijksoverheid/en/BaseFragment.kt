/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

abstract class BaseFragment @JvmOverloads constructor(
    @LayoutRes layout: Int,
    val factoryProducer: (() -> ViewModelProvider.Factory)? = null
) : Fragment(layout) {

    override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
        return factoryProducer?.invoke() ?: ViewModelFactory(requireContext().applicationContext)
    }
}
