package com.asoft.artsal.photo.business.text

import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.artsal.photo.editor.collage.maker.R
import com.asoft.artsal.photo.business.collage.customview.FramePhotoLayout

internal class Text(
    private val mPhotoEditorView: FramePhotoLayout,
    private val mMultiTouchListener: MultiTouchListener,
    private val mViewState: PhotoEditorViewState,
    private val mDefaultTextTypeface: Typeface?,
    private val mGraphicManager: GraphicManager
) : Graphic(
    context = mPhotoEditorView.context,
    graphicManager = mGraphicManager,
    viewType = ViewType.TEXT,
    layoutId = R.layout.view_collage_editor_text
) {

    private var mTextView: TextView? = null
    private var currentFontResourceId: Int? = null

    fun buildView(text: String?, styleBuilder: TextStyleBuilder?) {
        mTextView?.apply {
            this.text = text
            styleBuilder?.applyStyle(this)
            currentFontResourceId = styleBuilder?.getFontResourceId()
        }
    }

    private fun setupGesture() {
        val onGestureControl = buildGestureController(mPhotoEditorView, mViewState)
        mMultiTouchListener.setOnGestureControl(onGestureControl)
        val rootView = rootView
        val textView = rootView.findViewById<TextView>(R.id.tvPhotoEditorText)
        rootView.setOnTouchListener(mMultiTouchListener)
    }

    override fun setupView(rootView: View) {
        mTextView = rootView.findViewById(R.id.tvPhotoEditorText)
        mTextView?.run {
            gravity = Gravity.CENTER
            typeface = mDefaultTextTypeface
        }
    }

    override fun updateView(view: View) {
        val textInput = mTextView?.text.toString()
        val currentTextColor = mTextView?.currentTextColor ?: 0
        val photoEditorListener = mGraphicManager.onPhotoEditorListener
        photoEditorListener?.onEditTextChangeListener(view, textInput, currentTextColor, currentFontResourceId)
    }

    init {
        setupGesture()
    }
}