/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.items

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Path.Direction
import android.text.Layout
import android.text.Spanned
import android.text.style.LeadingMarginSpan
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.core.graphics.withTranslation

/**
 * Bullet with custom color and gap size, consistent on every API level
 */
class BulletPointSpan(
    @Px private val gapWidth: Int = 2,
    @Px private val bulletRadius: Float = 8.0f,
    @ColorInt private val color: Int = Color.BLACK,
    private val useColor: Boolean = color != Color.BLACK
) : LeadingMarginSpan {

    private val bulletPath: Path by lazy(LazyThreadSafetyMode.NONE) { Path() }

    override fun getLeadingMargin(first: Boolean): Int {
        return (bulletRadius * 2 + gapWidth).toInt()
    }

    override fun drawLeadingMargin(
        canvas: Canvas,
        paint: Paint,
        currentMarginLocation: Int,
        paragraphDirection: Int,
        lineTop: Int,
        lineBaseline: Int,
        lineBottom: Int,
        text: CharSequence,
        lineStart: Int,
        lineEnd: Int,
        isFirstLine: Boolean,
        layout: Layout
    ) {
        if ((text as Spanned).getSpanStart(this) == lineStart) {
            paint.withCustomColor {
                if (canvas.isHardwareAccelerated) {
                    // Bullet is slightly better to avoid aliasing artifacts on mdpi devices.
                    bulletPath.addCircle(0.0f, 0.0f, 1.2f * bulletRadius, Direction.CW)

                    canvas.withTranslation(
                        getCircleXLocation(currentMarginLocation, paragraphDirection),
                        getCircleYLocation(lineBaseline)
                    ) {
                        drawPath(bulletPath, paint)
                    }
                } else {
                    canvas.drawCircle(
                        getCircleXLocation(currentMarginLocation, paragraphDirection),
                        getCircleYLocation(lineBaseline),
                        bulletRadius,
                        paint
                    )
                }
            }
        }
    }

    private fun getCircleYLocation(lineBaseline: Int) =
        lineBaseline - bulletRadius * 1.1f // tiny bit above the baseline, otherwise it looks off

    private fun getCircleXLocation(currentMarginLocation: Int, paragraphDirection: Int) =
        bulletRadius / 2 + currentMarginLocation + paragraphDirection * bulletRadius

    private inline fun Paint.withCustomColor(block: () -> Unit) {
        val oldStyle = style
        val oldColor = if (useColor) color else Color.TRANSPARENT

        if (useColor) {
            color = this@BulletPointSpan.color
        }

        style = Paint.Style.FILL

        block()

        if (useColor) {
            color = oldColor
        }

        style = oldStyle
    }
}
