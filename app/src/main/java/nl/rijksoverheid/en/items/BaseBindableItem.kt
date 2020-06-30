/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.items

import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.xwray.groupie.viewbinding.BindableItem
import com.xwray.groupie.viewbinding.GroupieViewHolder

abstract class BaseBindableItem<T : ViewDataBinding> : BindableItem<T>() {
    override fun initializeViewBinding(view: View): T = DataBindingUtil.bind<T>(view)!!

    override fun bind(viewHolder: GroupieViewHolder<T>, position: Int, payloads: MutableList<Any>) {
        super.bind(viewHolder, position, payloads)
        viewHolder.binding.executePendingBindings()
    }

    override fun isClickable() = false
    override fun isLongClickable() = false
}
