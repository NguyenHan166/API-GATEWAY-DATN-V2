package com.asoft.artsal.photo.component

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat

open class OnSwipeTouchListener(c: Context) : View.OnTouchListener {
    private val gestureDetector: GestureDetectorCompat

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(motionEvent)
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD: Int = 50
        private val SWIPE_VELOCITY_THRESHOLD: Int = 50
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onClick()
            return super.onSingleTapUp(e)
        }
        override fun onDoubleTap(e: MotionEvent): Boolean {
            onDoubleClick()
            return super.onDoubleTap(e)
        }
        override fun onLongPress(e: MotionEvent) {
            onLongClick()
            super.onLongPress(e)
        }
//        override fun onFling(
//            e1: MotionEvent,
//            e2: MotionEvent,
//            velocityX: Float,
//            velocityY: Float
//        ): Boolean {
//            try {
//                if(e1 != null && e2 != null){
//                    val diffY = (e2.y - e1.y)
//                    val diffX = e2.x - e1.x
//                if (kotlin.math.abs(diffX) > kotlin.math.abs(diffY)) {
//                    if (kotlin.math.abs(diffX) > 1000 && kotlin.math.abs(velocityX) > 1000) {
//                        if (diffX > 0) {
//                            onSwipeRight()
//                        } else {
//                            onSwipeLeft()
//                        }
//                    }
//                } else {
//                    if (kotlin.math.abs(diffY) > SWIPE_THRESHOLD && kotlin.math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
//                        if (diffY - 10.0 < 0) {
//                            onSwipeUp()
//                        } else {
//                            onSwipeDown()
//                        }
//                    }
//                }
//                } else {
//                    return false
//                }
//            } catch (exception: Exception) {
//                exception.printStackTrace()
//            }
//            return false
//        }
    }
    open fun onSwipeRight() {}
    open fun onSwipeLeft() {}
    open fun onSwipeUp() {}
    open fun onSwipeDown() {}
    open fun onClick() {}
    private fun onDoubleClick() {}
    private fun onLongClick() {}
    init {
        gestureDetector = GestureDetectorCompat(c, GestureListener())
    }
}