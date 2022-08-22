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
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.model.DashboardItemRef
import nl.rijksoverheid.en.databinding.FragmentListBinding
import nl.rijksoverheid.en.items.DashboardCardItem
import nl.rijksoverheid.en.navigation.navigateCatchingErrors
import nl.rijksoverheid.en.util.ext.setExitSlideTransition

private const val KEY_NAVIGATED_TO_DASHBOARD_ITEM = "navigate_dashboard_item"

class DashboardOverviewFragment : BaseFragment(R.layout.fragment_list) {

    private val viewModel: DashboardViewModel by navGraphViewModels(
        R.id.nav_dashboard,
        factoryProducer = { defaultViewModelProviderFactory }
    )
    private val adapter = GroupAdapter<GroupieViewHolder>()
    private val section = DashboardOverviewSection().also {
        adapter.add(it)
    }

    private var navigatedToDashboardItem: Boolean = false

    private lateinit var binding: FragmentListBinding
    private val args: DashboardOverviewFragmentArgs by navArgs()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_NAVIGATED_TO_DASHBOARD_ITEM, navigatedToDashboardItem)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigatedToDashboardItem =
            savedInstanceState?.getBoolean(KEY_NAVIGATED_TO_DASHBOARD_ITEM, false) ?: false
        setExitSlideTransition()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentListBinding.bind(view)

        binding.toolbar.apply {
            setTitle(R.string.dashboard_title)
        }

        binding.content.adapter = adapter
        adapter.setOnItemClickListener { item, _ ->
            when (item) {
                is DashboardCardItem -> navigateToDashboardItem(item.dashboardItem.reference)
            }
        }

        viewModel.dashboardData.observe(viewLifecycleOwner) { result ->
            when (result) {
                DashboardDataResult.Error -> {
                    // TODO error state
                }
                is DashboardDataResult.Success -> section.updateDashboardData(requireContext(), result.data, ::navigateToMoreInfo)
            }
        }

        val reference = args.dashboardItemReference?.let {
            DashboardItemRef.valueOf(it)
        }
        if (reference != null && !navigatedToDashboardItem) {
            navigatedToDashboardItem = true
            findNavController().navigate(
                DashboardFragmentDirections.actionDashboardFragment(
                    reference,
                    fromDeeplink = true
                ),
                FragmentNavigatorExtras(binding.appbar to binding.appbar.transitionName)
            )
        }
    }

    private fun navigateToDashboardItem(dashboardItemReference: DashboardItemRef) {
        findNavController().navigateCatchingErrors(
            DashboardOverviewFragmentDirections.actionDashboardFragment(
                dashboardItemReference
            ),
            FragmentNavigatorExtras(binding.appbar to binding.appbar.transitionName)
        )
    }

    private fun navigateToMoreInfo() {
        val result = viewModel.dashboardData.value as? DashboardDataResult.Success
        result?.data?.moreInfoUrl?.let { moreInfoUrl ->
            val url = Uri.parse(moreInfoUrl)
            CustomTabsIntent.Builder().build().launchUrl(requireContext(), url)
        }
    }
}
