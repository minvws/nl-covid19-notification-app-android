/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import nl.rijksoverheid.en.R

/**
 * RecyclerView.ItemDecoration that adds margin below the first item
 */
class IllustrationSpaceDecoration : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.bottom =
                view.context.resources.getDimensionPixelOffset(R.dimen.space_below_illustration)
        }
    }
}
