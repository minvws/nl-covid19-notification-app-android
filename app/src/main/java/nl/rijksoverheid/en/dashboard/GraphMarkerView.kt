/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.dashboard

import android.annotation.SuppressLint
import android.content.Context
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ViewGraphMarkerBinding

@SuppressLint("ViewConstructor")
class GraphMarkerView(
    context: Context,
    private val graphWidth: () -> Int
) : MarkerView(context, R.layout.view_graph_marker) {

    private lateinit var binding: ViewGraphMarkerBinding

    init {
        isFocusable = true
    }

    override fun refreshContent(entry: Entry?, highlight: Highlight?) {
        binding = ViewGraphMarkerBinding.bind(this)

        binding.markerLabel.text = entry?.y?.toString()
        super.refreshContent(entry, highlight)
    }

    override fun getOffsetForDrawingAtPoint(posX: Float, posY: Float): MPPointF {
        val supposedX = width / 2 + posX
        val mpPointF = MPPointF()

        // Avoid marker being positioned outside the graph
        mpPointF.x = when {
            supposedX > graphWidth() -> -width.toFloat()
            posX - width / 2 < 0 -> 0f
            else -> -(width / 2).toFloat()
        }

        mpPointF.y = -posY - (resources.getDimensionPixelSize(R.dimen.activity_vertical_margin) / 2)
        return mpPointF
    }
}
