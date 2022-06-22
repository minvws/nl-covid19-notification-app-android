/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import nl.rijksoverheid.en.R

class AccessibleLineChart : LineChart {

    constructor (context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor (context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    private var touchEnabled = true
    private var selectedEntry: Entry? = null
    var selectedValueIcon = ContextCompat.getDrawable(context, R.drawable.ic_graph_dot_indicator)
    var selectedValueLabel: String? = null

    var accessibilityGraphDescription: String = ""

    init {
        // enable being detected by ScreenReader
        isFocusable = true

        ViewCompat.setAccessibilityDelegate(
            this,
            object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                    super.onInitializeAccessibilityNodeInfo(host, info)

                    info.setSource(host)
                    if (touchEnabled)
                        info.isClickable = true
                }
            }
        )

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

    override fun setTouchEnabled(enabled: Boolean) {
        super.setTouchEnabled(enabled)
        touchEnabled = enabled
    }

    private fun getAccessibilityDescription(): String {
        val lineData = lineData ?: return ""
        val yAxisValueFormatter = axisLeft.valueFormatter
        val xAxisValueFormatter = xAxis.valueFormatter

        val dataSet = lineData.getDataSetByIndex(0) as LineDataSet
        val startValue = dataSet.getEntryForIndex(0).y
        val endValue = dataSet.getEntryForIndex(dataSet.entryCount - 1).y
        val differencePercentage = startValue / endValue * 100

        // Min and max values...
        val minEntry = dataSet.values.minByOrNull { it.y } ?: return ""
        val minVal = yAxisValueFormatter.getFormattedValue(minEntry.y)
        val minValDate = DateTimeHelper.convertToLocalDate(minEntry.x.toLong()).formatDateShort(context)
        val maxEntry = dataSet.values.maxByOrNull { it.y } ?: return ""
        val maxVal = yAxisValueFormatter.getFormattedValue(maxEntry.y)
        val maxValDate = DateTimeHelper.convertToLocalDate(maxEntry.x.toLong()).formatDateShort(context)

        // Data range...
        val minRange = xAxisValueFormatter.getFormattedValue(lineData.xMin)
        val maxRange = xAxisValueFormatter.getFormattedValue(lineData.xMax)

        return when {
            startValue < endValue -> {
                context.getString(
                    R.string.dashboard_graph_content_description_increase,
                    accessibilityGraphDescription,
                    differencePercentage.toInt(),
                    minRange,
                    maxRange,
                    maxVal,
                    maxValDate,
                    minVal,
                    minValDate
                )
            }
            startValue > endValue -> {
                context.getString(
                    R.string.dashboard_graph_content_description_decrease,
                    accessibilityGraphDescription,
                    differencePercentage.toInt(),
                    minRange,
                    maxRange,
                    maxVal,
                    maxValDate,
                    minVal,
                    minValDate
                )
            }
            else -> {
                context.getString(
                    R.string.dashboard_graph_content_description_same,
                    accessibilityGraphDescription,
                    minRange,
                    maxRange,
                    maxVal,
                    maxValDate,
                    minVal,
                    minValDate
                )
            }
        }
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
