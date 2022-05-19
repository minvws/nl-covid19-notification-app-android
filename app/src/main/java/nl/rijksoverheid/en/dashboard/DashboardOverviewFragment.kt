/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.dashboard

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.model.DashboardItemRef
import nl.rijksoverheid.en.databinding.FragmentListBinding
import nl.rijksoverheid.en.items.DashboardCardItem
import nl.rijksoverheid.en.navigation.navigateCatchingErrors
import nl.rijksoverheid.en.util.ext.setExitSlideTransition

class DashboardOverviewFragment : BaseFragment(R.layout.fragment_list) {

    private val viewModel: DashboardViewModel by viewModels()
    private val adapter = GroupAdapter<GroupieViewHolder>()
    private val section = DashboardOverviewSection().also {
        adapter.add(it)
    }

    private lateinit var binding: FragmentListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setExitSlideTransition()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentListBinding.bind(view)

        binding.toolbar.apply {
            setTitle(R.string.dashboard_title)
            setNavigationIcon(R.drawable.ic_close)
            setNavigationContentDescription(R.string.cd_close)
        }

        binding.content.adapter = adapter
        adapter.setOnItemClickListener { item, _ ->
            when (item) {
                is DashboardCardItem -> navigateToDashboardItem(item.dashboardItem.reference)
            }
        }

        viewModel.dashboardData.observe(viewLifecycleOwner) { dashboardData ->
            dashboardData.data?.let {
                section.updateDashboardData(requireContext(), it, ::navigateToMoreInfo)
            }
        }
    }

    private fun navigateToDashboardItem(dashboardItemReference: DashboardItemRef) {
        findNavController().navigateCatchingErrors(
            DashboardOverviewFragmentDirections.actionDashboardFragment(dashboardItemReference, true),
            FragmentNavigatorExtras(binding.appbar to binding.appbar.transitionName)
        )
    }

    private fun navigateToMoreInfo() {
        viewModel.dashboardData.value?.data?.moreInfoUrl?.let { moreInfoUrl ->
            val url = Uri.parse(moreInfoUrl)
            CustomTabsIntent.Builder().build().launchUrl(requireContext(), url)
        }
    }
}
