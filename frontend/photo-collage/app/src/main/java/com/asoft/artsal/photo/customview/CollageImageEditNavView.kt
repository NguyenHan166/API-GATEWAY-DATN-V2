package com.asoft.artsal.photo.customview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.artsal.photo.editor.collage.maker.databinding.ViewCollageNavEditImageBinding
import com.asoft.artsal.photo.business.collage.customview.FrameImageView
import com.asoft.artsal.photo.business.collage.model.PhotoItem
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import timber.log.Timber

class CollageImageEditNavView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewCollageNavEditImageBinding =
        ViewCollageNavEditImageBinding.inflate(LayoutInflater.from(context), this, true)
    private var selectedImageView: FrameImageView? = null
    private var selectedPhotoItem: PhotoItem? = null

    interface OnImageEditNavListener {
        fun onCloseClicked()
        fun onSwapClicked(photoItem: PhotoItem, imageView: FrameImageView)
        fun onCropClicked(photoItem: PhotoItem, imageView: FrameImageView)
        fun onReplaceClicked(photoItem: PhotoItem, imageView: FrameImageView)
        fun onFilterClicked(photoItem: PhotoItem, imageView: FrameImageView)
    }

    private var listener: OnImageEditNavListener? = null

    init {
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.apply {
            btnClose.setOnClickListener {
                FirebaseEventUtils.logEventTracking(context, "edit_img_close")

                listener?.onCloseClicked()
                hide()
            }

            menuSwap.setOnClickListener {
                FirebaseEventUtils.logEventTracking(context, "edit_img_swap")

                selectedPhotoItem?.let { photoItem ->
                    selectedImageView?.let { imageView ->
                        listener?.onSwapClicked(photoItem, imageView)
                    }
                }
            }

            menuCrop.setOnClickListener {
                FirebaseEventUtils.logEventTracking(context, "edit_img_crop")

                selectedPhotoItem?.let { photoItem ->
                    selectedImageView?.let { imageView ->
                        listener?.onCropClicked(photoItem, imageView)
                    }
                }
            }

            menuReplace.setOnClickListener {
                FirebaseEventUtils.logEventTracking(context, "edit_img_replace")

                selectedPhotoItem?.let { photoItem ->
                    selectedImageView?.let { imageView ->
                        listener?.onReplaceClicked(photoItem, imageView)
                    }
                }
            }

            menuFilter.setOnClickListener {
                FirebaseEventUtils.logEventTracking(context, "edit_img_filter")

                selectedPhotoItem?.let { photoItem ->
                    selectedImageView?.let { imageView ->
                        listener?.onFilterClicked(photoItem, imageView)
                    }
                }
            }
        }
    }

    fun setOnImageEditNavListener(listener: OnImageEditNavListener) {
        this.listener = listener
    }

    fun show(photoItem: PhotoItem, imageView: FrameImageView) {
        FirebaseEventUtils.logEventTracking(context, "edit_img_view")

        Timber.Forest.d("Showing image edit nav for photo: ${photoItem.imagePath}")
        selectedPhotoItem = photoItem
        selectedImageView = imageView
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
        selectedPhotoItem = null
        selectedImageView = null
    }

    fun isVisible(): Boolean {
        return visibility == VISIBLE
    }

    fun getSelectedPhotoItem(): PhotoItem? = selectedPhotoItem

    fun getSelectedImageView(): FrameImageView? = selectedImageView
}