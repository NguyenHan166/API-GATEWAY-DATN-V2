package com.asoft.artsal.photo.component

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacingVertical: Int,
    private val spacingHorizontal: Int,
    private val includeEdge: Boolean,
    private val headerNum: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view) - headerNum // item position

        if (position >= 0) {
            val column = position % spanCount // item column
            if (includeEdge) {
                outRect.left =
                    spacingHorizontal - column * spacingHorizontal / spanCount // spacing - column * ((1f / spanCount) * spacing)
                outRect.right =
                    (column + 1) * spacingHorizontal / spanCount // (column + 1) * ((1f / spanCount) * spacing)

                if (position < spanCount) { // top edge
                    outRect.top = spacingVertical
                }
                outRect.bottom = spacingVertical // item bottom
            } else {
                outRect.left =
                    column * spacingHorizontal / spanCount // column * ((1f / spanCount) * spacing)
                outRect.right =
                    spacingHorizontal - (column + 1) * spacingHorizontal / spanCount // spacing - (column + 1) * ((1f /    spanCount) * spacing)
                outRect.bottom = spacingVertical
            }
        } else {
            outRect.left = 0
            outRect.right = 0
            outRect.top = 0
            outRect.bottom = 0
        }
    }
}