/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentListBinding
import nl.rijksoverheid.en.navigation.navigateCatchingErrors

class KeyShareOptionsFragment : BaseFragment(R.layout.fragment_list) {

    private val section = KeyShareOptionsSection()
    private val adapter = GroupAdapter<GroupieViewHolder>().apply { add(section) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition =
            TransitionInflater.from(context).inflateTransition(R.transition.slide_start)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentListBinding.bind(view)
        binding.toolbar.setTitle(R.string.lab_test_generic_toolbar_title)
        binding.content.adapter = adapter

        adapter.setOnItemClickListener { item, _ ->
            when (item) {
                KeyShareOptionItem.CoronaTest -> findNavController().navigateCatchingErrors(
                    KeyShareOptionsFragmentDirections.actionCoronaTestKeySharing(),
                    FragmentNavigatorExtras(binding.appbar to binding.appbar.transitionName)
                )
                KeyShareOptionItem.GGD -> findNavController().navigateCatchingErrors(
                    KeyShareOptionsFragmentDirections.actionLabTest(),
                    FragmentNavigatorExtras(binding.appbar to binding.appbar.transitionName)
                )
            }
        }
    }
}
