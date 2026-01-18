package com.asoft.artsal.photo.component

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class SpacingItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val spanCount = 2 // hoặc lấy từ layoutManager nếu cần động

        // Xác định cột
        val column = position % spanCount

        outRect.top = spacing
        outRect.bottom = 0

        // Chia đều spacing cho trái/phải
        outRect.left = if (column == 0) 0 else spacing / 2
        outRect.right = if (column == spanCount - 1) 0 else spacing / 2
    }
}