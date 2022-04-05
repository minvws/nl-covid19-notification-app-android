/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.dashboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ViewGraphMarkerBinding
import nl.rijksoverheid.en.util.formatToString

@SuppressLint("ViewConstructor")
class GraphMarkerView(
    context: Context,
    private val label: String,
    private val graphWidth: () -> Int
) : MarkerView(context, R.layout.view_graph_marker) {

    private lateinit var binding: ViewGraphMarkerBinding

    override fun refreshContent(entry: Entry?, highlight: Highlight?) {
        binding = ViewGraphMarkerBinding.bind(this)

        binding.markerLabel.text = SpannableStringBuilder()
            .append(label)
            .append(": ")
            .append(entry?.y?.formatToString(context), StyleSpan(Typeface.BOLD), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.markerLabel.maxWidth = graphWidth()

        super.refreshContent(entry, highlight)
    }

    override fun getOffsetForDrawingAtPoint(posX: Float, posY: Float): MPPointF {
        val defaultXOffset = -(width / 2).toFloat()
        val mpPointF = MPPointF()

        // Avoid marker being positioned outside the graph
        mpPointF.x = when {
            // Use posX as offset to position the marker against the left side of the graph
            posX - width / 2 < 0 -> -posX
            // Calculate the difference between the end of the graph and the end of the marker
            // and use is as the offset + the default offset to position the marker against the right side of the graph
            posX + width / 2 > graphWidth() -> graphWidth() - (posX + width / 2) + defaultXOffset
            // Use halve of the marker width to center the marker above the selected value
            else -> defaultXOffset
        }

        mpPointF.y = -posY
        return mpPointF
    }
}
