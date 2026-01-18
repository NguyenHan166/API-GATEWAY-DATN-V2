package com.asoft.artsal.photo.extensions

import androidx.annotation.CheckResult
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate


@CheckResult
@OptIn(ExperimentalCoroutinesApi::class)
fun ViewPager2.pageScrollStateChanges(): Flow<Int> = callbackFlow {
    checkMainThread()
    val callback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrollStateChanged(state: Int) {
            trySend(state)
        }
    }
    registerOnPageChangeCallback(callback)
    awaitClose { unregisterOnPageChangeCallback(callback) }
}.conflate()

@CheckResult
@OptIn(ExperimentalCoroutinesApi::class)
fun ViewPager2.pageScrollEvents(): Flow<ViewPager2PageScrollEvent> = callbackFlow {
    checkMainThread()
    val callback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            trySend(
                ViewPager2PageScrollEvent(
                    view = this@pageScrollEvents,
                    position = position,
                    positionOffset = positionOffset,
                    positionOffsetPixel = positionOffsetPixels
                )
            )
        }
    }
    registerOnPageChangeCallback(callback)
    awaitClose { unregisterOnPageChangeCallback(callback) }
}.conflate()

data class ViewPager2PageScrollEvent(
    val view: ViewPager2,
    val position: Int,
    val positionOffset: Float,
    val positionOffsetPixel: Int
)

internal val ViewPager2.isIdle get() = scrollState == ViewPager2.SCROLL_STATE_IDLE

val ViewPager2.recyclerView: RecyclerView?
    inline get() = children.find { it is RecyclerView } as? RecyclerView


fun ViewPager2.reduceDragSensitivity() {
    val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
    recyclerViewField.isAccessible = true
    val recyclerView = recyclerViewField.get(this) as RecyclerView

    val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
    touchSlopField.isAccessible = true
    val touchSlop = touchSlopField.get(recyclerView) as Int
    touchSlopField.set(recyclerView, touchSlop * 6) // "6" was obtained experimentally
}

fun ViewPager2.reduceDragSensitivityBy(f: Int = 4) {
    val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
    recyclerViewField.isAccessible = true
    val recyclerView = recyclerViewField.get(this) as RecyclerView

    val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
    touchSlopField.isAccessible = true
    val touchSlop = touchSlopField.get(recyclerView) as Int
    touchSlopField.set(recyclerView, touchSlop * f) // "6" was obtained experimentally
}