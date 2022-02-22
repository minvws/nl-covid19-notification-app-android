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
import com.github.mikephil.charting.data.LineDataSet
import nl.rijksoverheid.en.R

fun LineChart.applyCardViewStyling() {
    this.axisLeft.isEnabled = false
    this.axisRight.isEnabled = false
    this.xAxis.isEnabled = false
    this.legend.isEnabled = false
    this.description.isEnabled = false
    this.setTouchEnabled(false)
}

fun LineDataSet.applyLineStyling(context: Context) {
    mode = LineDataSet.Mode.HORIZONTAL_BEZIER
    lineWidth = 2f
    color = ContextCompat.getColor(context, R.color.dashboard_graph_line)
    fillColor = ContextCompat.getColor(context, R.color.dashboard_graph_fill)
    setDrawFilled(true)
    setDrawValues(false)
    setDrawCircles(false)
    setDrawVerticalHighlightIndicator(true)
    setDrawHorizontalHighlightIndicator(false)
}