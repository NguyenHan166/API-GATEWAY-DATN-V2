package com.asoft.artsal.photo.business.text

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.text.TextUtils
import android.view.GestureDetector
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.artsal.photo.editor.collage.maker.R
import com.asoft.artsal.photo.business.collage.customview.FramePhotoLayout
import com.asoft.artsal.photo.business.sticker.Sticker
import com.asoft.artsal.photo.business.text.shape.ShapeBuilder

/**
 *
 * This class in initialize by [com.asoft.artsal.photo.business.text.PhotoEditor.Builder] using a builder pattern with multiple
 * editing attributes
 *
 */
internal class PhotoEditorImpl @SuppressLint("ClickableViewAccessibility") constructor(
    builder: PhotoEditor.Builder
) : PhotoEditor {
    private val photoEditorView: FramePhotoLayout = builder.photoEditorView
    private val viewState: PhotoEditorViewState = PhotoEditorViewState()
    private val imageView: ImageView = builder.imageView
    private val deleteView: View? = builder.deleteView
    private val drawingView: DrawingView = builder.drawingView
    private val mBrushDrawingStateListener: BrushDrawingStateListener =
        BrushDrawingStateListener(builder.photoEditorView, viewState)
    private val mBoxHelper: BoxHelper = BoxHelper(builder.photoEditorView, viewState)
    private var mOnPhotoEditorListener: OnPhotoEditorListener? = null
    private val isTextPinchScalable: Boolean = builder.isTextPinchScalable
    private val mDefaultTextTypeface: Typeface? = builder.textTypeface
    private val mDefaultEmojiTypeface: Typeface? = builder.emojiTypeface
    private val mGraphicManager: GraphicManager = GraphicManager(builder.photoEditorView, viewState)
    private val context: Context = builder.context

    override fun addImage(desiredImage: Bitmap) {
        val multiTouchListener = getMultiTouchListener(true)
        val sticker = Sticker(photoEditorView, multiTouchListener, viewState, mGraphicManager)
        sticker.buildView(desiredImage)
        addToEditor(sticker)
    }

    override fun addText(text: String, colorCodeTextView: Int) {
        addText(null, text, colorCodeTextView)
    }

    override fun addText(textTypeface: Typeface?, text: String, colorCodeTextView: Int) {
        val styleBuilder = TextStyleBuilder()
        styleBuilder.withTextColor(colorCodeTextView)
        if (textTypeface != null) {
            styleBuilder.withTextFont(textTypeface)
        }
        addText(text, styleBuilder)
    }

    override fun addText(text: String, styleBuilder: TextStyleBuilder?) {
        drawingView.enableDrawing(false)
        val multiTouchListener = getMultiTouchListener(isTextPinchScalable)
        val textGraphic = Text(
            photoEditorView,
            multiTouchListener,
            viewState,
            mDefaultTextTypeface,
            mGraphicManager
        )
        textGraphic.buildView(text, styleBuilder)
        addToEditor(textGraphic)
    }

    override fun editText(view: View, inputText: String, colorCode: Int) {
        editText(view, null, inputText, colorCode)
    }

    override fun editText(view: View, textTypeface: Typeface?, inputText: String, colorCode: Int) {
        val styleBuilder = TextStyleBuilder()
        styleBuilder.withTextColor(colorCode)
        if (textTypeface != null) {
            styleBuilder.withTextFont(textTypeface)
        }
        editText(view, inputText, styleBuilder)
    }

    override fun editText(view: View, inputText: String, styleBuilder: TextStyleBuilder?) {
        val inputTextView = view.findViewById<TextView>(R.id.tvPhotoEditorText)
        if (inputTextView != null && viewState.containsAddedView(view) && !TextUtils.isEmpty(
                inputText
            )
        ) {
            inputTextView.text = inputText
            styleBuilder?.applyStyle(inputTextView)
            mGraphicManager.updateView(view)
        }
    }

    private fun addToEditor(graphic: Graphic) {
        clearHelperBox()
        mGraphicManager.addView(graphic)
        // Change the in-focus view
        viewState.currentSelectedView = graphic.rootView
    }

    /**
     * Create a new instance and scalable touchview
     *
     * @param isPinchScalable true if make pinch-scalable, false otherwise.
     * @return scalable multitouch listener
     */
    private fun getMultiTouchListener(isPinchScalable: Boolean): MultiTouchListener {
        return MultiTouchListener(
            deleteView,
            photoEditorView,
            imageView,
            isPinchScalable,
            mOnPhotoEditorListener,
            viewState
        )
    }

    override fun undo(): Boolean {
        return mGraphicManager.undoView()
    }

    override val isUndoAvailable get() = viewState.addedViewsCount > 0

    override fun redo(): Boolean {
        return mGraphicManager.redoView()
    }

    override val isRedoAvailable get() = mGraphicManager.redoStackCount > 0
//
    override fun clearAllViews() {
        mBoxHelper.clearAllViews(drawingView)
    }

    init {
        drawingView.setBrushViewChangeListener(mBrushDrawingStateListener)
        val mDetector = GestureDetector(
            context,
            PhotoEditorImageViewListener(
                viewState,
                object : PhotoEditorImageViewListener.OnSingleTapUpCallback {
                    override fun onSingleTapUp() {
                        clearHelperBox()
                    }
                }
            )
        )
        imageView.setOnTouchListener { _, event ->
            mOnPhotoEditorListener?.onTouchSourceImage(event)
            mDetector.onTouchEvent(event)
        }
//        photoEditorView.setClipSourceImage(builder.clipSourceImage)
    }

    override fun clearHelperBox() {
        mBoxHelper.clearHelperBox()
    }

//    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
//    override suspend fun saveAsFile(
//        imagePath: String,
//        saveSettings: SaveSettings
//    ): SaveFileResult = withContext(Dispatchers.Main) {
//        photoEditorView.saveFilter()
//        val photoSaverTask = PhotoSaverTask(photoEditorView, mBoxHelper, saveSettings)
//        return@withContext photoSaverTask.saveImageAsFile(imagePath)
//    }

//    override suspend fun saveAsBitmap(
//        saveSettings: SaveSettings
//    ): Bitmap = withContext(Dispatchers.Main) {
//        photoEditorView.saveFilter()
//        val photoSaverTask = PhotoSaverTask(photoEditorView, mBoxHelper, saveSettings)
//        return@withContext photoSaverTask.saveImageAsBitmap()
//    }

//    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
//    override fun saveAsFile(
//        imagePath: String,
//        saveSettings: SaveSettings,
//        onSaveListener: PhotoEditor.OnSaveListener
//    ) {
//        GlobalScope.launch(Dispatchers.Main) {
//            when (val result = saveAsFile(imagePath, saveSettings)) {
//                is SaveFileResult.Success -> onSaveListener.onSuccess(imagePath)
//                is SaveFileResult.Failure -> onSaveListener.onFailure(result.exception)
//            }
//        }
//    }

//    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
//    override fun saveAsFile(imagePath: String, onSaveListener: PhotoEditor.OnSaveListener) {
//        saveAsFile(imagePath, SaveSettings.Builder().build(), onSaveListener)
//    }

//    override fun saveAsBitmap(saveSettings: SaveSettings, onSaveBitmap: OnSaveBitmap) {
//        GlobalScope.launch(Dispatchers.Main) {
//            val bitmap = saveAsBitmap(saveSettings)
//            onSaveBitmap.onBitmapReady(bitmap)
//        }
//    }

//    override fun saveAsBitmap(onSaveBitmap: OnSaveBitmap) {
//        saveAsBitmap(SaveSettings.Builder().build(), onSaveBitmap)
//    }

    override fun setOnPhotoEditorListener(onPhotoEditorListener: OnPhotoEditorListener) {
        mOnPhotoEditorListener = onPhotoEditorListener
        mGraphicManager.onPhotoEditorListener = mOnPhotoEditorListener
        mBrushDrawingStateListener.setOnPhotoEditorListener(mOnPhotoEditorListener)
    }

//    override val isCacheEmpty: Boolean
//        get() = !isUndoAvailable && !isRedoAvailable

    // region Shape
    override fun setShape(shapeBuilder: ShapeBuilder) {
        drawingView.currentShapeBuilder = shapeBuilder
    } // endregion

}