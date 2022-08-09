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
import androidx.navigation.fragment.navArgs
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.model.DashboardItemRef
import nl.rijksoverheid.en.databinding.FragmentListBinding
import nl.rijksoverheid.en.navigation.navigateCatchingErrors
import nl.rijksoverheid.en.util.ext.setExitSlideTransition
import nl.rijksoverheid.en.util.ext.setSlideTransition

/**
 * Fragment for displaying Corona statistics.
 */
class DashboardFragment : BaseFragment(R.layout.fragment_list) {

    private val args: DashboardFragmentArgs by navArgs()

    private val viewModel: DashboardViewModel by viewModels()
    private val adapter = GroupAdapter<GroupieViewHolder>()
    private val section = DashboardSection().also {
        adapter.add(it)
    }

    private lateinit var binding: FragmentListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (args.showEnterTransition) {
            setSlideTransition()
        } else {
            setExitSlideTransition()
        }
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

        viewModel.dashboardData.observe(viewLifecycleOwner) { dashboardData ->
            when (dashboardData) {
                is DashboardDataResult.Success -> section.updateDashboardData(
                    requireContext(),
                    args.dashboardItemReference,
                    dashboardData.data,
                    ::navigateToDashboardItem,
                    ::navigateToMoreInfo
                )
                else -> {
                    // TODO error state
                }
            }
        }
    }

    private fun navigateToDashboardItem(dashboardItemReference: DashboardItemRef) {
        enterTransition = exitTransition
        findNavController().navigateCatchingErrors(
            DashboardFragmentDirections.actionDashboardFragment(dashboardItemReference, true),
            FragmentNavigatorExtras(binding.appbar to binding.appbar.transitionName)
        )
    }

    private fun navigateToMoreInfo() {
        val dashboardData = viewModel.dashboardData.value as? DashboardDataResult.Success
        dashboardData?.data?.getDashboardItem(args.dashboardItemReference)?.let {
            val url = Uri.parse(it.moreInfoUrl)
            CustomTabsIntent.Builder().build().launchUrl(requireContext(), url)
        }
    }
}
