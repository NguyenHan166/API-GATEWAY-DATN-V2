package com.asoft.artsal.photo.business.sticker

import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import com.artsal.photo.editor.collage.maker.R
import com.asoft.artsal.photo.business.collage.customview.FramePhotoLayout
import com.asoft.artsal.photo.business.text.Graphic
import com.asoft.artsal.photo.business.text.GraphicManager
import com.asoft.artsal.photo.business.text.MultiTouchListener
import com.asoft.artsal.photo.business.text.PhotoEditorViewState
import com.asoft.artsal.photo.business.text.ViewType

internal class Sticker(
    private val mPhotoEditorView: FramePhotoLayout,
    private val mMultiTouchListener: MultiTouchListener,
    private val mViewState: PhotoEditorViewState,
    graphicManager: GraphicManager?
) : Graphic(
    context = mPhotoEditorView.context,
    graphicManager = graphicManager,
    viewType = ViewType.IMAGE,
    layoutId = R.layout.view_collage_editor_sticker
) {
    private var imageView: ImageView? = null
    fun buildView(desiredImage: Bitmap?) {
        imageView?.setImageBitmap(desiredImage)
    }

    private fun setupGesture() {
        val onGestureControl = buildGestureController(mPhotoEditorView, mViewState)
        mMultiTouchListener.setOnGestureControl(onGestureControl)
        val rootView = rootView
        rootView.setOnTouchListener(mMultiTouchListener)
    }

    override fun setupView(rootView: View) {
        imageView = rootView.findViewById(R.id.imgCollageEditorSticker)
    }

    init {
        setupGesture()
    }
}