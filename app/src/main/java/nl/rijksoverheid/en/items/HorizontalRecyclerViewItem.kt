/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.items

import android.view.View
import com.xwray.groupie.Group
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemHorizontalRecyclerviewBinding

class HorizontalRecyclerViewItem(
    private val children: List<Group>
) : BaseBindableItem<ItemHorizontalRecyclerviewBinding>() {

    private val section = Section()
    private val adapter = GroupAdapter<GroupieViewHolder>().apply { add(section) }

    override fun getLayout() = R.layout.item_horizontal_recyclerview

    override fun bind(viewBinding: ItemHorizontalRecyclerviewBinding, position: Int) {
        section.update(children)
        viewBinding.content.adapter = adapter
    }
}