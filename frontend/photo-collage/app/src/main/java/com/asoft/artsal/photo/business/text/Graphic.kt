package com.asoft.artsal.photo.business.text

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.artsal.photo.editor.collage.maker.R
import com.asoft.artsal.photo.business.collage.customview.FramePhotoLayout
import com.asoft.artsal.photo.extensions.visible

internal abstract class Graphic(
    val context: Context,
    val layoutId: Int,
    val viewType: ViewType,
    val graphicManager: GraphicManager?
) {

    val rootView: View

    open fun updateView(view: View) {
        //Optional for subclass to override
    }

    init {
        if (layoutId == 0) {
            throw UnsupportedOperationException("Layout id cannot be zero. Please define a layout")
        }
        rootView = LayoutInflater.from(context).inflate(layoutId, null)
        setupView(rootView)
        setupRemoveView(rootView)
        setupRotateView(rootView)
        if(viewType == ViewType.TEXT) {
            setupEditView(rootView)
        }
    }


    private fun setupRemoveView(rootView: View) {
        //We are setting tag as ViewType to identify what type of the view it is
        //when we remove the view from stack i.e onRemoveViewListener(ViewType viewType, int numberOfAddedViews);
        rootView.tag = viewType
        val imgClose = rootView.findViewById<ImageView>(R.id.imgPhotoEditorClose)
        imgClose?.setOnClickListener { graphicManager?.removeView(this@Graphic) }
    }

    private fun setupRotateView(rootView: View) {
        val imgRotate = rootView.findViewById<ImageView>(R.id.imgPhotoEditorRotate)
        imgRotate?.setOnClickListener {
            graphicManager?.performQuickRotate(this@Graphic)
        }
    }

    private fun setupEditView(rootView: View) {
        val imgEdit = rootView.findViewById<ImageView>(R.id.imgEdit)
        imgEdit?.setOnClickListener {
            updateView(rootView)
        }
    }

    protected fun toggleSelection() {
        val frmBorder = rootView.findViewById<View>(R.id.frmBorder)
        val imgClose = rootView.findViewById<View>(R.id.imgPhotoEditorClose)
        val imgRotate = rootView.findViewById<View>(R.id.imgPhotoEditorRotate)
        val imgEdit = rootView.findViewById<View>(R.id.imgEdit)
        if (frmBorder != null) {
            frmBorder.setBackgroundResource(R.drawable.rounded_border_tv)
            frmBorder.tag = true
        }
        if (imgClose != null) {
            imgClose.visibility = View.VISIBLE
            imgRotate.visible()
            if(viewType == ViewType.TEXT) {
                imgEdit.visible()
            }
        }
    }

    protected fun buildGestureController(
        photoEditorView: FramePhotoLayout,
        viewState: PhotoEditorViewState
    ): MultiTouchListener.OnGestureControl {
        val boxHelper = BoxHelper(photoEditorView, viewState)
        return object : MultiTouchListener.OnGestureControl {
            override fun onClick() {
                boxHelper.clearHelperBox()
                toggleSelection()
                viewState.currentSelectedView = rootView
            }

            override fun onLongClick() {
            }
        }
    }

    open fun setupView(rootView: View) {}
}