/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util.ext

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.Utils
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.dashboard.GraphMarkerView
import nl.rijksoverheid.en.util.AccessibleLineChart
import nl.rijksoverheid.en.util.formatDate
import java.time.Instant
import java.time.LocalDateTime.ofInstant
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.pow

fun LineChart.applyCardViewStyling() {
    axisLeft.isEnabled = false
    axisRight.isEnabled = false
    xAxis.isEnabled = false
    legend.isEnabled = false
    description.isEnabled = false

    setTouchEnabled(false)

    val horizontalOffset = resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin).toFloat()
    setViewPortOffsets(horizontalOffset, 0f, horizontalOffset, 0f)

    // Fix: SetViewPortOffSets are not applied the first time within a Recyclerview
    post { invalidate() }
}

fun AccessibleLineChart.applyDashboardStyling(
    context: Context,
    dataSet: LineDataSet,
    maxValue: Float,
    markerLabel: String,
    formatValue: (Float) -> String
) {
    axisRight.isEnabled = false
    legend.isEnabled = false
    description.isEnabled = false

    val upperbound = calculateUpperbound(maxValue)
    val yAsGridLines = calculateAmountOfGridLines(maxValue)
    val axisTextSize = 12f * resources.configuration.fontScale
    val axisTextColor = ContextCompat.getColor(context, R.color.dashboard_graph_axis_text)

    with(axisLeft) {
        yOffset = -(Utils.convertPixelsToDp(textSize) / 2)
        setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
        setDrawAxisLine(false)
        setDrawGridLines(true)
        setLabelCount(yAsGridLines, true)
        textSize = axisTextSize
        textColor = axisTextColor
        axisMinimum = 0f
        axisMaximum = upperbound
        valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                return if (value == axisMinimum || value == axisMaximum)
                    formatValue(value)
                else ""
            }
        }
    }

    with(xAxis) {
        position = XAxis.XAxisPosition.BOTTOM
        setDrawAxisLine(false)
        setDrawGridLines(false)
        setAvoidFirstLastClipping(true)
        setLabelCount(2, true)
        textSize = axisTextSize
        textColor = axisTextColor
        axisMinimum = dataSet.xMin
        axisMaximum = dataSet.xMax
        valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return ofInstant(
                    Instant.ofEpochSecond(value.toLong()),
                    ZoneId.systemDefault()
                ).formatDate(context)
            }
        }
    }

    setTouchEnabled(true)
    setPinchZoom(false)
    setScaleEnabled(false)

    isHighlightPerDragEnabled = true
    isHighlightPerTapEnabled = true

    selectedValueLabel = markerLabel
    marker = GraphMarkerView(context, markerLabel) {
        Pair(this.width, this.height)
    }

    dataSet.applyLineStyling(context)

    val bottomOffset = Utils.convertDpToPixel(
        axisTextSize + context.resources.getDimension(R.dimen.dashboard_graph_bottom_offset)
    )
    setViewPortOffsets(
        0f,
        resources.getDimensionPixelSize(R.dimen.dashboard_graph_top_margin).toFloat(),
        0f,
        bottomOffset
    )

    // Fix: SetViewPortOffSets are not applied the first time within a Recyclerview
    post { invalidate() }
}

fun LineDataSet.applyLineStyling(context: Context) {
    mode = LineDataSet.Mode.CUBIC_BEZIER
    lineWidth = 2f
    color = ContextCompat.getColor(context, R.color.color_primary)
    fillColor = ContextCompat.getColor(context, R.color.dashboard_graph_fill)
    cubicIntensity = 0.1f

    setDrawFilled(true)
    setDrawValues(false)
    setDrawCircles(false)

    highLightColor = ContextCompat.getColor(context, R.color.dashboard_graph_highlight_line)
    val highlightLength = Utils.convertDpToPixel(2f)
    enableDashedHighlightLine(highlightLength, highlightLength, 0f)
    setDrawVerticalHighlightIndicator(true)
    setDrawHorizontalHighlightIndicator(false)
}

@VisibleForTesting
private fun numberOfDigits(maxValue: Float) = when {
    maxValue < 1f -> 1f
    else -> log10(maxValue).toInt() + 1
}.toFloat()

@VisibleForTesting
private fun calculateUpperbound(maxValue: Float): Float {
    val numberOfDigits = numberOfDigits(maxValue)
    val orderOfMagnitude = 10f.pow(numberOfDigits - 1)
    return ceil(maxValue / orderOfMagnitude) * orderOfMagnitude
}

@VisibleForTesting
private fun calculateAmountOfGridLines(maxValue: Float): Int {
    val numberOfDigits = numberOfDigits(maxValue)
    val orderOfMagnitude = 10f.pow(numberOfDigits - 1)
    val upperBound = calculateUpperbound(maxValue)
    return (upperBound / orderOfMagnitude).toInt() + 1
}
