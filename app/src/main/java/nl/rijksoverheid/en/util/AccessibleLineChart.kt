/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import android.content.Context
import android.util.AttributeSet
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import nl.rijksoverheid.en.R

class AccessibleLineChart : LineChart {

    constructor (context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor (context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    private var selectedEntry: Entry? = null
    var selectedValueIcon = ContextCompat.getDrawable(context, R.drawable.ic_graph_dot_indicator)
    var selectedValueLabel: String? = null

    var accessibilityGraphDescription: String = ""

    init {
        // enable being detected by ScreenReader
        isFocusable = true

        setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(entry: Entry?, h: Highlight?) {
                selectedEntry?.apply { icon = null }
                selectedEntry = entry
                selectedEntry?.apply { icon = selectedValueIcon }

                sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED)
            }

            override fun onNothingSelected() {
                selectedEntry?.apply { icon = null }
                selectedEntry = null
            }
        })
    }

    override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent): Boolean {
        super.dispatchPopulateAccessibilityEvent(event)
        if (selectedEntry == null)
            event.text.add(getAccessibilityDescription())
        else
            getSelectedValueAccessibilityDescription()?.let { event.text.add(it) }

        return true
    }

    private fun getAccessibilityDescription(): String {
        val lineData = lineData

        // Min and max values...
        val yAxisValueFormatter = axisLeft.valueFormatter
        val minVal = yAxisValueFormatter.getFormattedValue(lineData.yMin)
        val maxVal = yAxisValueFormatter.getFormattedValue(lineData.yMax)

        // Data range...
        val xAxisValueFormatter =
            xAxis.valueFormatter
        val minRange = xAxisValueFormatter.getFormattedValue(lineData.xMin)
        val maxRange = xAxisValueFormatter.getFormattedValue(lineData.xMax)
        return context.getString(
            R.string.dashboard_graph_content_description,
            accessibilityGraphDescription, minVal, maxVal, minRange, maxRange
        )
    }

    private fun getSelectedValueAccessibilityDescription(): String? {
        return selectedEntry?.let { entry ->
            context.getString(
                R.string.dashboard_graph_selected_value_content_description,
                entry.y.formatToString(context),
                selectedValueLabel,
                xAxis.valueFormatter.getFormattedValue(entry.x)
            )
        }
    }
}
