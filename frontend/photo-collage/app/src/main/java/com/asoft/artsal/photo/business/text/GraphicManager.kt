package com.asoft.artsal.photo.business.text

import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.asoft.artsal.photo.business.collage.customview.FramePhotoLayout
import com.ironsource.gr

internal class GraphicManager(
    private val mPhotoEditorView: FramePhotoLayout,
    private val mViewState: PhotoEditorViewState
) {

    var onPhotoEditorListener: OnPhotoEditorListener? = null

    val redoStackCount
        get() = mViewState.redoViewsCount

    fun addView(graphic: Graphic) {
        val view = graphic.rootView
        val params = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        mPhotoEditorView.addView(view, params)
        mViewState.addAddedView(view)

        if (redoStackCount > 0) {
            mViewState.clearRedoViews()
        }

        onPhotoEditorListener?.onAddViewListener(
            graphic.viewType,
            mViewState.addedViewsCount
        )
    }

    fun removeView(graphic: Graphic) {
        val view = graphic.rootView
        if (mViewState.containsAddedView(view)) {
            mPhotoEditorView.removeView(view)
            mViewState.removeAddedView(view)
            mViewState.pushRedoView(view)
            onPhotoEditorListener?.onRemoveViewListener(
                graphic.viewType,
                mViewState.addedViewsCount
            )
        }
    }

    fun updateView(view: View) {
        mPhotoEditorView.updateViewLayout(view, view.layoutParams)
        mViewState.replaceAddedView(view)
    }

    fun undoView(): Boolean {
        if (mViewState.addedViewsCount > 0) {
            val removeView = mViewState.getAddedView(
                mViewState.addedViewsCount - 1
            )
            if (removeView is DrawingView) {
                return removeView.undo() || (mViewState.addedViewsCount != 0)
            } else {
                mViewState.removeAddedView(mViewState.addedViewsCount - 1)
                mPhotoEditorView.removeView(removeView)
                mViewState.pushRedoView(removeView)
            }
            when (val viewTag = removeView.tag) {
                is ViewType -> onPhotoEditorListener?.onRemoveViewListener(
                    viewTag,
                    mViewState.addedViewsCount
                )
            }
        }
        return mViewState.addedViewsCount != 0
    }

    fun redoView(): Boolean {
        if (redoStackCount > 0) {
            val redoView = mViewState.getRedoView(redoStackCount - 1)

            if (redoView is DrawingView) {
                val result = redoView.redo()
                return result || redoStackCount > 0
            } else {
                mViewState.popRedoView()
                mPhotoEditorView.addView(redoView)
                mViewState.addAddedView(redoView)
            }

            val viewTag = redoView.tag
            if (viewTag is ViewType) {
                onPhotoEditorListener?.onAddViewListener(viewTag, mViewState.addedViewsCount)
            }
        }

        return redoStackCount > 0
    }

    /**
     * Thực hiện xoay nhanh text theo quy tắc:
     * - 0° -> 90°: Xoay thành 90° (nghiêng theo chiều dọc)
     * - 90° < x < 180°: Xoay thành 180° (lật ngược)
     * - 180° < x < 270°: Xoay thành 270° (nghiêng ngược chiều dọc)
     * - 270° < x < 360°: Xoay về 0° (bình thường)
     */
    fun performQuickRotate(graphic: Graphic) {
        val rootView = graphic.rootView
        val currentRotation = rootView.rotation
        val normalizedRotation = normalizeAngle(currentRotation)

        val targetRotation = when {
            normalizedRotation >= 0f && normalizedRotation < 90f -> 90f
            normalizedRotation >= 90f && normalizedRotation < 180f -> 180f
            normalizedRotation >= 180f && normalizedRotation < 270f -> 270f
            else -> 0f
        }

        // animation rotate
        rootView.animate()
            .rotation(targetRotation)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    /**
     * Chuẩn hóa góc về khoảng 0-360 độ
     */
    private fun normalizeAngle(angle: Float): Float {
        var normalizedAngle = angle % 360f
        if (normalizedAngle < 0) {
            normalizedAngle += 360f
        }
        return normalizedAngle
    }
}