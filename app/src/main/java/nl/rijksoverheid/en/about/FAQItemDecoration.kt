/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.about

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.Dimension
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.xwray.groupie.GroupieViewHolder
import kotlin.math.roundToInt

private val ATTRS = intArrayOf(android.R.attr.listDivider)

class FAQItemDecoration(context: Context, @Dimension val startOffset: Int) :
    RecyclerView.ItemDecoration() {

    private val bounds = Rect()
    private val divider: Drawable

    init {
        val a: TypedArray = context.obtainStyledAttributes(ATTRS)
        val divider = a.getDrawable(0)
        if (divider == null) {
            throw IllegalStateException("@android:attr/listDivider was not set in the theme used for this decoration")
        } else {
            this.divider = divider
        }
        a.recycle()
    }

    private fun isFAQItem(view: View, parent: RecyclerView): Boolean {
        val item = (parent.getChildViewHolder(view) as? GroupieViewHolder)?.item
        return item is FAQItem || item is AboutFAQItem
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        canvas.save()
        val left: Int
        val right: Int
        if (parent.clipToPadding) {
            left = parent.paddingLeft
            right = parent.width - parent.paddingRight
            canvas.clipRect(
                left, parent.paddingTop, right,
                parent.height - parent.paddingBottom
            )
        } else {
            left = 0
            right = parent.width
        }

        val rtlLayout = ViewCompat.getLayoutDirection(parent) == ViewCompat.LAYOUT_DIRECTION_RTL
        val adaptedLeft = if (rtlLayout) left else left + startOffset
        val adaptedRight = if (rtlLayout) right - startOffset else right

        val childCount: Int = parent.childCount
        if (childCount > 1) {
            for (i in 0 until (childCount - 1)) {
                val child1: View = parent.getChildAt(i)
                val child2: View = parent.getChildAt(i + 1)
                if (isFAQItem(child1, parent) && isFAQItem(child2, parent)) {
                    parent.getDecoratedBoundsWithMargins(child1, bounds)
                    val bottom: Int = bounds.bottom + child1.translationY.roundToInt()
                    val top: Int = bottom - divider.intrinsicHeight
                    divider.setBounds(adaptedLeft, top, adaptedRight, bottom)
                    divider.draw(canvas)
                }
            }
        }
        canvas.restore()
    }
}
