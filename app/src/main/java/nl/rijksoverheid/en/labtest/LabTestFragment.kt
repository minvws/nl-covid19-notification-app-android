/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.about.FAQItem
import nl.rijksoverheid.en.databinding.FragmentListBinding
import nl.rijksoverheid.en.onboarding.HowItWorksFragmentDirections

class LabTestFragment : BaseFragment(R.layout.fragment_list) {
    private val viewModel: LabTestViewModel by viewModels()
    private val section = LabTestSection { viewModel.retry() }
    private val adapter = GroupAdapter<GroupieViewHolder>().apply { add(section) }

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

        viewModel.keyState.observe(viewLifecycleOwner) { keyState -> section.update(keyState) }

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
