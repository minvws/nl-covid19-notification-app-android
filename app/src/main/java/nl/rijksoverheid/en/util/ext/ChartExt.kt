/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.util.ext

import android.content.Context
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.Utils
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.dashboard.GraphMarkerView
import nl.rijksoverheid.en.util.formatDate
import java.time.Instant
import java.time.LocalDateTime.ofInstant
import java.time.ZoneId

fun LineChart.applyCardViewStyling() {
    axisLeft.isEnabled = false
    axisRight.isEnabled = false
    xAxis.isEnabled = false
    legend.isEnabled = false
    description.isEnabled = false

    setTouchEnabled(false)

    val horizontalOffset = resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin).toFloat()
    setViewPortOffsets(horizontalOffset, 0f, horizontalOffset, 0f)

    //Fix: SetViewPortOffSets are not applied the first time within a Recyclerview
    post { invalidate() }
}

fun LineChart.applyDashboardStyling(
    context: Context,
    dataSet: LineDataSet
) {
    axisRight.isEnabled = false
    legend.isEnabled = false
    description.isEnabled = false

    with(axisLeft) {
        yOffset = -(Utils.convertPixelsToDp(textSize) / 2)
        setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
        setDrawAxisLine(false)
        setDrawGridLines(false)
        setLabelCount(2, true)
        axisMinimum = 0f
        axisMaximum = 60000f
    }

    with(xAxis) {
        position = XAxis.XAxisPosition.BOTTOM
        setDrawAxisLine(false)
        setDrawGridLines(false)
        setAvoidFirstLastClipping(true)
        setLabelCount(2, true)
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
    marker = GraphMarkerView(context) {
        this.width
    }

    dataSet.applyLineStyling(context)

    val verticalOffset = resources.getDimensionPixelSize(R.dimen.activity_vertical_margin).toFloat()
    setViewPortOffsets(0f, verticalOffset, 0f, verticalOffset)

    //Fix: SetViewPortOffSets are not applied the first time within a Recyclerview
    post { invalidate() }
}

fun LineDataSet.applyLineStyling(context: Context) {
    mode = LineDataSet.Mode.HORIZONTAL_BEZIER
    lineWidth = 2f
    color = ContextCompat.getColor(context, R.color.dashboard_graph_line)
    fillColor = ContextCompat.getColor(context, R.color.dashboard_graph_fill)


    setDrawFilled(true)
    setDrawValues(false)
    setDrawCircles(false)

    highLightColor = ContextCompat.getColor(context, R.color.dashboard_graph_highlight_line)
    val highlightLength = Utils.convertDpToPixel(2f)
    enableDashedHighlightLine(highlightLength, highlightLength, 0f)
    setDrawVerticalHighlightIndicator(true)
    setDrawHorizontalHighlightIndicator(false)
}