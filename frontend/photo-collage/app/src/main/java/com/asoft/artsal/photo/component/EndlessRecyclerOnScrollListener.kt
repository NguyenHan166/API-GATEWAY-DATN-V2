package com.asoft.artsal.photo.component

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

abstract class EndlessRecyclerOnScrollListener : RecyclerView.OnScrollListener(),
    OnBottomListener {
    private var HIDE_THRESHOLD = 10

    enum class LAYOUT_MANAGER_TYPE {
        LINEAR, GRID, STAGGERED_GRID, NONE
    }

    protected var layoutManagerType: LAYOUT_MANAGER_TYPE? = null
    private var firstVisibleItemPosition = 0
    private var lastVisibleItemPosition = 0
    private var isScrollDown = false

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        val layoutManager = recyclerView.layoutManager
        if (layoutManagerType == null) {
            layoutManagerType = when (layoutManager) {
                is LinearLayoutManager -> {
                    LAYOUT_MANAGER_TYPE.LINEAR
                }
                is GridLayoutManager -> {
                    LAYOUT_MANAGER_TYPE.GRID
                }
                is StaggeredGridLayoutManager -> {
                    LAYOUT_MANAGER_TYPE.STAGGERED_GRID
                }
                else -> {
                    LAYOUT_MANAGER_TYPE.NONE
                }
            }
        }
        when (layoutManagerType) {
            LAYOUT_MANAGER_TYPE.LINEAR -> {
                firstVisibleItemPosition =
                    (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
            }
            LAYOUT_MANAGER_TYPE.GRID -> {
                firstVisibleItemPosition =
                    (layoutManager as GridLayoutManager).findFirstVisibleItemPosition()
                lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
            }
            LAYOUT_MANAGER_TYPE.STAGGERED_GRID -> {
                val staggeredGridLayoutManager = layoutManager as StaggeredGridLayoutManager
                val firstPositions = IntArray(staggeredGridLayoutManager.spanCount)
                val lastPositions = IntArray(staggeredGridLayoutManager.spanCount)
                staggeredGridLayoutManager.findFirstVisibleItemPositions(firstPositions)
                staggeredGridLayoutManager.findLastVisibleItemPositions(lastPositions)
                firstVisibleItemPosition = findMin(firstPositions)
                lastVisibleItemPosition = findMax(lastPositions)
            }
            else -> {}
        }
        onScroll(firstVisibleItemPosition, lastVisibleItemPosition, dx, dy)
        isScrollDown = dy > 0
        if (lastVisibleItemPosition > HIDE_THRESHOLD) {
            if (isScrollDown) {
                onHide()
            } else {
                onShow()
            }
        } else onHide()
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)
        val layoutManager = recyclerView.layoutManager
        val visibleItemCount = layoutManager?.childCount
        val totalItemCount = layoutManager?.itemCount
        if ((visibleItemCount
                ?: 0) > 0 && newState == RecyclerView.SCROLL_STATE_IDLE && lastVisibleItemPosition >= (totalItemCount?.minus(
                3
            ) ?: 0)
        ) {
            onBottom()
        }
    }

    override fun onBottom() {
    }

    override fun onScroll(firstVisible: Int, lastVisible: Int, dx: Int, dy: Int) {}
    override fun onShow() {}
    override fun onHide() {}
    private fun findMax(lastPositions: IntArray): Int {
        var max = lastPositions[0]
        for (value in lastPositions) {
            if (value > max) {
                max = value
            }
        }
        return max
    }

    private fun findMin(lastPositions: IntArray): Int {
        var min = lastPositions[0]
        for (value in lastPositions) {
            if (value < min) {
                min = value
            }
        }
        return min
    }

    companion object {
        private const val TAG = "EndlessRecyclerOnScrollListener"
    }
}