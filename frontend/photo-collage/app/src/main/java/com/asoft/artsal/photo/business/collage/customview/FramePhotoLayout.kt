package com.asoft.artsal.photo.business.collage.customview

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.DragEvent
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import com.asoft.artsal.photo.business.collage.model.PhotoItem
import com.asoft.artsal.photo.business.collage.utils.ImageDecoder
import com.asoft.artsal.photo.business.collage.utils.ImageUtils
import com.asoft.artsal.photo.business.text.DrawingView
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import com.ironsource.nu
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@SuppressLint("ViewConstructor")
class FramePhotoLayout(
    context: Context,
    var mPhotoItems: List<PhotoItem>
) : RelativeLayout(context), FrameImageView.OnImageClickListener {

    private var mOnDragListener: OnDragListener = OnDragListener { v, event ->
        if (event.action == DragEvent.ACTION_DROP) {
            var target: FrameImageView? = v as FrameImageView
            val selectedView = getSelectedFrameImageView(target!!, event)
            if (selectedView != null) {
                target = selectedView
                val dragged = event.localState as FrameImageView
                var targetPath: Uri? = target.photoItem.imagePath
                var draggedPath: Uri? = dragged.photoItem.imagePath
                if (targetPath == null) targetPath = Uri.EMPTY
                if (draggedPath == null) draggedPath = Uri.EMPTY
                if (targetPath != draggedPath) target.swapImage(dragged)
            }
        }
        true
    }

    internal var drawingView: DrawingView
        private set

    private val mItemImageViews: MutableList<FrameImageView> = ArrayList()
    private var mViewWidth: Int = 0
    private var mViewHeight: Int = 0
    private var mOutputScaleRatio = 1f
    private var backgroundColor: Int = Color.WHITE

    private var mSelectedView: FrameImageView? = null

    /**
     * Source image which want to edit
     * @return source ImageView
     */
    var source: ImageView? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    interface OnBuildCompleteListener {
        fun onBuildComplete()
        fun onBuildError(error: Throwable)
    }

    private var onBuildCompleteListener: OnBuildCompleteListener? = null

    private val isNotLargeThan1Gb: Boolean
        get() {
            val memoryInfo = ImageUtils.getMemoryInfo(context)
            return memoryInfo.totalMem > 0 && memoryInfo.totalMem / 1048576.0 <= 1024
        }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        drawingView = DrawingView(context)
        val brushParam = setupDrawingView()
        addView(drawingView, brushParam)
    }

    private fun getSelectedFrameImageView(
        target: FrameImageView,
        event: DragEvent
    ): FrameImageView? {
        val dragged = event.localState as FrameImageView
        val leftMargin = (mViewWidth * target.photoItem.bound.left).toInt()
        val topMargin = (mViewHeight * target.photoItem.bound.top).toInt()
        val globalX = leftMargin + event.x
        val globalY = topMargin + event.y
        for (idx in mItemImageViews.indices.reversed()) {
            val view = mItemImageViews[idx]
            val x = globalX - mViewWidth * view.photoItem.bound.left
            val y = globalY - mViewHeight * view.photoItem.bound.top
            if (view.isSelected(x, y)) {
                return if (view === dragged) {
                    null
                } else {
                    view
                }
            }
        }
        return null
    }

    fun saveInstanceState(outState: Bundle) {
        for (view in mItemImageViews)
            view.saveInstanceState(outState)
    }

    fun restoreInstanceState(savedInstanceState: Bundle) {
        for (view in mItemImageViews)
            view.restoreInstanceState(savedInstanceState)
    }

    suspend fun buildAsync(
        viewWidth: Int,
        viewHeight: Int,
        outputScaleRatio: Float,
        space: Float = 0f,
        corner: Float = 0f
    ) {
        try {
            Timber.d("buildAsync - Starting build process")

            mItemImageViews.clear()
            removeAllViews()
            if (viewWidth < 1 || viewHeight < 1) {
                Timber.e("buildAsync - Invalid dimensions: $viewWidth x $viewHeight")
                return
            }

            super.setBackgroundColor(backgroundColor)
            mViewWidth = viewWidth
            mViewHeight = viewHeight
            mOutputScaleRatio = outputScaleRatio

            // Configure image decoder
            if (mPhotoItems.size > 4 || isNotLargeThan1Gb) {
                ImageDecoder.SAMPLER_SIZE = 1024
            } else {
                ImageDecoder.SAMPLER_SIZE = 1600
            }

            Timber.d("buildAsync - Creating ${mPhotoItems.size} image views")

            for (item in mPhotoItems) {
                val imageView = createFrameImageView(item, outputScaleRatio, space, corner)
                mItemImageViews.add(imageView)
            }

            Timber.d("buildAsync - All image views created, loading images...")

            val imageLoadingDeferreds = mItemImageViews.map { imageView ->
                coroutineScope.async(Dispatchers.IO) {
                    try {
                        if (imageView.photoItem.imagePath != null) {
                            Timber.d("buildAsync - Loading image: ${imageView.photoItem.imagePath}")
                            val bitmap = ImageDecoder.decodeFileToBitmap(
                                context,
                                imageView.photoItem.imagePath!!
                            )

                            if (bitmap != null && !bitmap.isRecycled) {
                                withContext(Dispatchers.Main) {
                                    imageView.image = bitmap
                                    imageView.resetImageMatrix()
                                    Timber.d("buildAsync - Image loaded successfully for ${imageView.photoItem.imagePath}")
                                }
                            } else {
                                Timber.e("buildAsync - Failed to load bitmap for ${imageView.photoItem.imagePath}")
                            }
                        } else {
                            Timber.w("buildAsync - No image path for view")
                        }
                    } catch (e: Exception) {
                        Timber.e("buildAsync - Failed to load image for ${imageView.photoItem.imagePath}")
                    }
                }
            }

            imageLoadingDeferreds.awaitAll()

            this.source = setImageView()

            onBuildCompleteListener?.onBuildComplete()

        } catch (error: Exception) {
            Timber.e(error, "buildAsync - Build failed")
            onBuildCompleteListener?.onBuildError(error)
            throw error
        }
    }

    @JvmOverloads
    fun build(
        viewWidth: Int,
        viewHeight: Int,
        outputScaleRatio: Float,
        space: Float = 0f,
        corner: Float = 0f
    ) {
        coroutineScope.launch {
            try {
                buildAsync(viewWidth, viewHeight, outputScaleRatio, space, corner)
            } catch (error: Exception) {
                FirebaseEventUtils.recordException(error)
                Timber.e(error, "build - Build failed")
            }
        }
    }

    override fun setBackgroundColor(color: Int) {
        backgroundColor = color
        super.setBackgroundColor(color)
        invalidate()
        requestLayout()
    }

    fun setBackgroundColorDirect(color: Int) {
        backgroundColor = color
        setBackgroundColor(color)
        post {
            invalidate()
            requestLayout()
        }
    }

    fun setBackgroundColorWithDrawable(color: Int) {
        backgroundColor = color
        val drawable = color.toDrawable()
        background = drawable
        post {
            invalidate()
            requestLayout()
        }
    }

    fun setSpace(space: Float, corner: Float) {
        for (img in mItemImageViews)
            img.setSpace(space, corner)
    }

    private fun createFrameImageView(
        item: PhotoItem,
        outputScaleRatio: Float,
        space: Float,
        corner: Float
    ): FrameImageView {
        val imageView = FrameImageView(context, item, false) // Không load ảnh trong constructor

        val leftMargin = (mViewWidth * item.bound.left).toInt()
        val topMargin = (mViewHeight * item.bound.top).toInt()
        val frameWidth: Int = if (item.bound.right == 1f) {
            mViewWidth - leftMargin
        } else {
            (mViewWidth * item.bound.width() + 0.5f).toInt()
        }
        val frameHeight: Int = if (item.bound.bottom == 1f) {
            mViewHeight - topMargin
        } else {
            (mViewHeight * item.bound.height() + 0.5f).toInt()
        }

        imageView.init(frameWidth.toFloat(), frameHeight.toFloat(), outputScaleRatio, space, corner)
        imageView.setOnImageClickListener(this)

        if (mPhotoItems.size > 1) {
            imageView.setOnDragListener(mOnDragListener)
        }

        val params = LayoutParams(frameWidth, frameHeight)
        params.leftMargin = leftMargin
        params.topMargin = topMargin
        imageView.originalLayoutParams = params
        addView(imageView, params)

        return imageView
    }

    private fun addPhotoItemView(
        item: PhotoItem,
        outputScaleRatio: Float,
        space: Float,
        corner: Float
    ): FrameImageView {
        val imageView = FrameImageView(context, item)
        val leftMargin = (mViewWidth * item.bound.left).toInt()
        val topMargin = (mViewHeight * item.bound.top).toInt()
        val frameWidth: Int = if (item.bound.right == 1f) {
            mViewWidth - leftMargin
        } else {
            (mViewWidth * item.bound.width() + 0.5f).toInt()
        }

        val frameHeight: Int = if (item.bound.bottom == 1f) {
            mViewHeight - topMargin
        } else {
            (mViewHeight * item.bound.height() + 0.5f).toInt()
        }

        imageView.init(frameWidth.toFloat(), frameHeight.toFloat(), outputScaleRatio, space, corner)
        imageView.setOnImageClickListener(this)
        if (mPhotoItems.size > 1)
            imageView.setOnDragListener(mOnDragListener)

        val params = LayoutParams(frameWidth, frameHeight)
        params.leftMargin = leftMargin
        params.topMargin = topMargin
        imageView.originalLayoutParams = params
        addView(imageView, params)
        return imageView
    }

    @Throws(OutOfMemoryError::class)
    fun createImage(): Bitmap? {
        try {
            if (mViewWidth <= 0 || mViewHeight <= 0) {
                Timber.e("createImage - Invalid dimensions: $mViewWidth x $mViewHeight")
            }

            if (mOutputScaleRatio <= 0) {
                Timber.e("createImage - Invalid scale ratio: $mOutputScaleRatio")
            }

            val bitmapWidth = (mOutputScaleRatio * mViewWidth).toInt()
            val bitmapHeight = (mOutputScaleRatio * mViewHeight).toInt()

            Timber.d("createImage - Creating bitmap: ${bitmapWidth}x${bitmapHeight}")

            val template = createBitmap(bitmapWidth, bitmapHeight)

            Timber.d("createImage - Bitmap created successfully: ${template.width}x${template.height}")

            val canvas = Canvas(template)
            canvas.drawColor(backgroundColor)

            var drawnImagesCount = 0
            Timber.d("createImage - Processing ${mItemImageViews.size} image views")

            for (view in mItemImageViews) {
                Timber.d("createImage - Checking view: image=${view.image}, isRecycled=${view.image?.isRecycled}, path=${view.photoItem.imagePath}")
                Timber.d("createImage - View position: left=${view.left}, top=${view.top}, width=${view.width}, height=${view.height}")

                if (view.image != null && !view.image!!.isRecycled) {
                    val left = (view.left * mOutputScaleRatio).toInt()
                    val top = (view.top * mOutputScaleRatio).toInt()
                    val width = (view.width * mOutputScaleRatio).toInt()
                    val height = (view.height * mOutputScaleRatio).toInt()

                    Timber.d("createImage - Drawing at: left=$left, top=$top, width=$width, height=$height")

                    canvas.saveLayer(
                        left.toFloat(),
                        top.toFloat(),
                        (left + width).toFloat(),
                        (top + height).toFloat(),
                        Paint()
                    )
                    canvas.translate(left.toFloat(), top.toFloat())
                    canvas.clipRect(0, 0, width, height)
                    view.drawOutputImage(canvas)
                    canvas.restore()

                    drawnImagesCount++
                    Timber.d("createImage - Successfully drawn image $drawnImagesCount")
                } else {
                    Timber.w("createImage - Skipping view: image=${view.image}, isRecycled=${view.image?.isRecycled}, path=${view.photoItem.imagePath}")
                }
            }
            return template

        } catch (error: OutOfMemoryError) {
            FirebaseEventUtils.recordException(Exception(error))
            return null
        } catch (error: Exception) {
            FirebaseEventUtils.recordException(Exception(error))
            return null
        }
    }

    private fun setupDrawingView(): LayoutParams {
        drawingView.visibility = GONE
        drawingView.id = shapeSrcId
        // Align drawing view to the size of image view
        val params = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.addRule(CENTER_IN_PARENT, TRUE)
        params.addRule(ALIGN_TOP, imgSrcId)
        params.addRule(ALIGN_BOTTOM, imgSrcId)
        params.addRule(ALIGN_LEFT, imgSrcId)
        params.addRule(ALIGN_RIGHT, imgSrcId)
        return params
    }

    override fun onLongClickImage(view: FrameImageView) {
        if (mPhotoItems.size > 1) {
            view.tag = """x=${0f},y=${0f},path=${view.photoItem.imagePath}"""
            val item = ClipData.Item(view.tag as CharSequence)
            val mimeTypes = arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
            val dragData = ClipData(view.tag.toString(), mimeTypes, item)
            val myShadow = DragShadowBuilder(view)
            view.startDragAndDrop(dragData, myShadow, view, 0)
        }
    }

    override fun onDoubleClickImage(view: FrameImageView) {

    }

    override fun onClickImage(view: FrameImageView) {
        if (selectedImageView === view) {
            clearSelection()
            (onPhotoSelectedListener as? OnSelectionAwareListener)?.onSelectionCleared()
            return
        }
        for (img in mItemImageViews) {
            img.setSelected(img == view)
        }
        selectedImageView = view
        onPhotoSelectedListener?.onPhotoSelected(view.photoItem, view)
    }

    suspend fun replaceSelectedImage(uri: Uri) {
        selectedImageView?.let { view ->
            view.photoItem.imagePath = uri
            view.image = ImageDecoder.decodeFileToBitmap(context, uri)
            view.resetImageMatrix()
        }
    }

    fun swapImages(photoItem1: PhotoItem, photoItem2: PhotoItem) {
        val view1 = mItemImageViews.find { it.photoItem == photoItem1 }
        val view2 = mItemImageViews.find { it.photoItem == photoItem2 }

        if (view1 != null && view2 != null) {
            view1.swapImage(view2)
            Timber.d("Swapped images: ${photoItem1.imagePath} <-> ${photoItem2.imagePath}")
        } else {
            Timber.e("Cannot find views for swap: view1=${view1 != null}, view2=${view2 != null}")
        }
    }

    fun updateSelectedImageWithFilter(bitmap: Bitmap) {
        selectedImageView?.let { view ->
            val currentImageMatrix = Matrix(view.imageMatrix)
            val currentScaleMatrix = Matrix()
            view.mTouchHandler?.scaleMatrix?.let { scaleMatrix ->
                currentScaleMatrix.set(scaleMatrix)
            }

            view.image = bitmap

            view.mImageMatrix.set(currentImageMatrix)
            view.mScaleMatrix.set(currentScaleMatrix)
            view.mTouchHandler?.setMatrices(view.mImageMatrix, view.mScaleMatrix)

            view.invalidate()
        } ?: run {
            Timber.d(
                "updateSelectedImageWithFilter - selectedImageView is null!"
            )
        }
    }

    fun updateSelectedImageWithUri(uri: Uri, onImageUpdated: ((Bitmap) -> Unit)? = null) {
        selectedImageView?.let { view ->
            coroutineScope.launch {
                try {
                    view.photoItem.imagePath = uri
                    val newBitmap = ImageDecoder.decodeFileToBitmap(context, uri)
                    view.image = newBitmap
                    view.resetImageMatrix()
                    view.invalidate()
                    onImageUpdated?.invoke(newBitmap ?: return@launch)
                } catch (e: Exception) {
                    FirebaseEventUtils.recordException(e)
                }
            }
        } ?: run {
            Timber.d("updateSelectedImageWithUri - selectedImageView is null!")
        }
    }

    private fun setImageView(): ImageView {
        val imageView = ImageView(context)
        try {
            val bitmap = createImage()

            val imageView = ImageView(context)
            bitmap?.let { imageView.setImageBitmap(it) }
        } catch (error: Exception) {
            FirebaseEventUtils.recordException(error)
        }
        return imageView
    }

    interface OnPhotoSelectedListener {
        fun onPhotoSelected(photoItem: PhotoItem, view: FrameImageView)
    }

    interface OnSelectionAwareListener : OnPhotoSelectedListener {
        fun onSelectionCleared()
    }

    private var onPhotoSelectedListener: OnPhotoSelectedListener? = null
    fun setOnPhotoSelectedListener(listener: OnPhotoSelectedListener) {
        this.onPhotoSelectedListener = listener
    }

    private var selectedImageView: FrameImageView? = null

    fun clearSelection() {
        for (img in mItemImageViews) {
            img.setSelected(false)
        }
        selectedImageView = null
        invalidate()
    }

    fun destroy() {
        coroutineScope.cancel()
        mItemImageViews.clear()
        onPhotoSelectedListener = null
    }

    companion object {
        private const val TAG = "PhotoEditorView"
        private const val imgSrcId = 1
        private const val shapeSrcId = 2
        private const val glFilterId = 3
    }
}
