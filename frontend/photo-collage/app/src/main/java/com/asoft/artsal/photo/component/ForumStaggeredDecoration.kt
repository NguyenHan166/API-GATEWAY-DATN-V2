package com.asoft.artsal.photo.component


import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class ForumStaggeredDecoration(context: Context, private val padding: Int) :
    RecyclerView.ItemDecoration() {
    var space: Int = padding / 2

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val gridLayoutManager = parent.layoutManager as StaggeredGridLayoutManager?
        val layoutParams = view.layoutParams as StaggeredGridLayoutManager.LayoutParams

        if (gridLayoutManager != null && parent.adapter != null) {
            val spanCount = gridLayoutManager.spanCount.toFloat()
            val spanIndex = layoutParams.getSpanIndex()
            outRect.left = if (spanIndex == 0) padding else space
            outRect.right = if (spanIndex.toFloat() == spanCount - 1) padding else space
            outRect.top = if (position <= spanCount) padding else space
            val itemCount = parent.adapter?.itemCount ?: 0
            outRect.bottom = if (position < itemCount - spanCount) space else padding
        }
    }

    private fun isHeader(viewType: Int): Boolean {
        when (viewType) {
            else -> return false
        }
    }
}