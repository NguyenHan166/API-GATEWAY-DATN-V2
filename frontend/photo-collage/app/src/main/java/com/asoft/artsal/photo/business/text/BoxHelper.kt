package com.asoft.artsal.photo.business.text

import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.artsal.photo.editor.collage.maker.R
import com.asoft.artsal.photo.business.collage.customview.FramePhotoLayout
import com.asoft.artsal.photo.extensions.gone


internal class BoxHelper(
    private val mPhotoEditorView: FramePhotoLayout,
    private val mViewState: PhotoEditorViewState
) {
    fun clearHelperBox() {
        for (i in 0 until mPhotoEditorView.childCount) {
            val childAt = mPhotoEditorView.getChildAt(i)
            val frmBorder = childAt.findViewById<FrameLayout>(R.id.frmBorder)
            frmBorder?.setBackgroundResource(0)
            val imgClose = childAt.findViewById<ImageView>(R.id.imgPhotoEditorClose)
            imgClose?.visibility = View.GONE
            val imgRotate = childAt.findViewById<ImageView>(R.id.imgPhotoEditorRotate)
            imgRotate?.gone()
            val imgEdit = childAt.findViewById<ImageView>(R.id.imgEdit)
            imgEdit?.gone()
        }
        mViewState.clearCurrentSelectedView()
    }

    fun clearAllViews(drawingView: DrawingView?) {
        for (i in 0 until mViewState.addedViewsCount) {
            mPhotoEditorView.removeView(mViewState.getAddedView(i))
        }
        drawingView?.let {
            if (mViewState.containsAddedView(it)) {
                mPhotoEditorView.addView(it)
            }
        }

        mViewState.clearAddedViews()
        mViewState.clearRedoViews()
        drawingView?.clearAll()
    }
}