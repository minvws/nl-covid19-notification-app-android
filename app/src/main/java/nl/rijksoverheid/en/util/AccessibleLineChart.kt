/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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
                    info.isClickable = touchEnabled
                }
            }
        )

        setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(entry: Entry?, h: Highlight?) {
                selectedEntry?.apply { icon = null }
                selectedEntry = entry
                selectedEntry?.apply { icon = selectedValueIcon }

                sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED)
            }

            override fun onNothingSelected() {
                selectedEntry?.apply { icon = null }
                selectedEntry = null
            }
        })
    }

    override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent): Boolean {
        super.dispatchPopulateAccessibilityEvent(event)
        if (selectedEntry == null) {
            event.text.add(getAccessibilityDescription())
        } else {
            getSelectedValueAccessibilityDescription()?.let { event.text.add(it) }
        }

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
        val startValueY = dataSet.getEntryForIndex(0).y
        val endValueY = dataSet.getEntryForIndex(dataSet.entryCount - 1).y
        val differencePercentage = startValueY / endValueY * 100

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
            startValueY < endValueY -> {
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
            startValueY > endValueY -> {
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handled = super.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_CANCEL || event.action == MotionEvent.ACTION_UP) {
            requestDisallowInterceptTouchEventFromParent(false)
        } else if (handled) {
            // prevent (scrolling) parents to intercept touch events while we're handling it
            requestDisallowInterceptTouchEventFromParent(true)
        }
        return handled
    }

    private fun requestDisallowInterceptTouchEventFromParent(disallow: Boolean) {
        var viewParent = parent as? ViewGroup
        while (viewParent != null) {
            parent.requestDisallowInterceptTouchEvent(disallow)
            viewParent = viewParent.parent as? ViewGroup
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
