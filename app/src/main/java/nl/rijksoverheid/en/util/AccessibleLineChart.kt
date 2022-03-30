/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.util

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.accessibility.AccessibilityEvent
import com.github.mikephil.charting.charts.LineChart
import timber.log.Timber
import java.util.Locale

class AccessibleLineChart: LineChart {

    constructor (context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) :super(context, attrs)
    constructor (context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    init {
        // enable being detected by ScreenReader
        isFocusable = true
    }

    val accessibilitySummaryDescription: String = ""

    override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent): Boolean {
        val completed = super.dispatchPopulateAccessibilityEvent(event)
        Timber.d("Dispatch called for Chart <View> and completed as $completed")
        event.text.add(getAccessibilityDescription())

        // Add the user generated summary after the dynamic summary is complete.
        if (!TextUtils.isEmpty(accessibilitySummaryDescription)) {
            event.text.add(accessibilitySummaryDescription)
        }
        return true
    }

    private fun getAccessibilityDescription(): String {
        val lineData = lineData
        val numberOfPoints = lineData.entryCount

        // Min and max values...
        val yAxisValueFormatter = axisLeft.valueFormatter
        val minVal = yAxisValueFormatter.getFormattedValue(lineData.yMin)
        val maxVal = yAxisValueFormatter.getFormattedValue(lineData.yMax)

        // Data range...
        val xAxisValueFormatter =
            xAxis.valueFormatter
        val minRange = xAxisValueFormatter.getFormattedValue(lineData.xMin)
        val maxRange = xAxisValueFormatter.getFormattedValue(lineData.xMax)
        val entries = if (numberOfPoints == 1) "entry" else "entries"
        return String.format(
            Locale.getDefault(), "The line chart has %d %s. " +
                "The minimum value is %s and maximum value is %s." +
                "Data ranges from %s to %s.",
            numberOfPoints, entries, minVal, maxVal, minRange, maxRange
        )
    }
}