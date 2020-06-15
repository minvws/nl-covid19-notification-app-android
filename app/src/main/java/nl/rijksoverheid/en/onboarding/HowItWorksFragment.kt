/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.onboarding

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.about.FAQItem
import nl.rijksoverheid.en.databinding.FragmentListBinding

class HowItWorksFragment : BaseFragment(R.layout.fragment_list) {
    private val adapter = GroupAdapter<GroupieViewHolder>().apply { add(HowItWorksSection()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition =
            TransitionInflater.from(context).inflateTransition(R.transition.slide_start)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentListBinding.bind(view)

        binding.toolbar.apply {
            setTitle(R.string.onboarding_how_it_works_toolbar_title)
            setNavigationIcon(R.drawable.ic_close)
            setNavigationContentDescription(R.string.cd_close)
            setNavigationOnClickListener { findNavController().popBackStack() }
        }
        binding.content.adapter = adapter

        adapter.setOnItemClickListener { item, _ ->
            if (item is FAQItem) {
                findNavController().navigate(
                    HowItWorksFragmentDirections.actionHowItWorksDetail(item.id),
                    FragmentNavigatorExtras(binding.appbar to binding.appbar.transitionName)
                )
            }
        }
    }
}
