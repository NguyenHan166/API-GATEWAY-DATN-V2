package com.asoft.artsal.photo.component

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class LinearSpacingItemDecoration(
    private val isHorizontal: Boolean = true,
    private val spacing: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position: Int = parent.getChildAdapterPosition(view)  // item position
        if (position > 0) {
            if (isHorizontal) {
                outRect.left = spacing
            } else {
                outRect.top = spacing
            }
        }
    }
}