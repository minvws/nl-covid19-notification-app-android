/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.dashboard

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentDashboardBinding
import nl.rijksoverheid.en.util.ext.applyDashboardStyling
import nl.rijksoverheid.en.util.ext.applyLineStyling
/**
 * Fragment for displaying Corona statistics.
 */
class DashboardFragment : BaseFragment(R.layout.fragment_dashboard) {

    private val args: DashboardFragmentArgs by navArgs()

    private val viewModel: DashboardViewModel by viewModels()

    private lateinit var binding: FragmentDashboardBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentDashboardBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel


        binding.lineChart.apply {

            val entries = args.dashboardItem.values
                .map { Entry(it.timestamp.toFloat(), it.value.toFloat()) }
                .sortedBy { it.x }
            val dataSet = LineDataSet(entries, "")
            data = LineData(dataSet)

            applyDashboardStyling(context, dataSet)

            //val horizontalOffset = resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin).toFloat()
            //setViewPortOffsets(horizontalOffset, 0f, horizontalOffset, 0f)

            //Fix: SetViewPortOffSets are not applied the first time
            //post { invalidate() }
        }

        /*
        val chartViewModel = ChartViewModel(
            chartDataPoints = args.dashboardItem.values.mapIndexed { index, item ->
                ScrubPointViewModel(
                    value = item.value,
                    timestamp = item.timestamp,
                    index = index,
                    talkbackString = item.value.toString()
                ) {
                    // On Swipe Callback
                }
            },
            strokeColor = R.color.dashboard_graph_line,
            fillColor = R.color.dashboard_graph_fill,
            benchmark =54225.0,
            benchmarkColor = R.color.color_error,
            contentDescription = "",
            priceHint = 0
        )
        binding.chartView.setViewModel(chartViewModel)

         */
    }
}
