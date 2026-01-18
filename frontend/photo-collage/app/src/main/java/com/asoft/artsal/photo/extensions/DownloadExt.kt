package com.asoft.artsal.photo.extensions

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Download image from URL and convert to Bitmap
 * Note: Glide operations must be called on Main thread
 */
suspend fun downloadImageAsBitmap(context: android.content.Context, url: String): Bitmap? {
    // Validate URL
    if (url.isBlank()) {
        Timber.w("downloadImageAsBitmap: Empty URL")
        return null
    }

    return withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val isCompleted = AtomicBoolean(false)
            
            Timber.d("downloadImageAsBitmap: Starting download from URL: $url")
            
            try {
                Glide.with(context)
                    .asBitmap()
                    .load(url)
                    .listener(object : RequestListener<Bitmap> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Bitmap>,
                            isFirstResource: Boolean
                        ): Boolean {
                            Timber.e(e, "Failed to download image from URL: $url")
                            if (isCompleted.compareAndSet(false, true) && continuation.isActive) {
                                continuation.resume(null)
                            }
                            return true // Prevent CustomTarget from being called
                        }

                        override fun onResourceReady(
                            resource: Bitmap,
                            model: Any,
                            target: Target<Bitmap>,
                            dataSource: com.bumptech.glide.load.DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false // Let CustomTarget handle it
                        }
                    })
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            Timber.d("downloadImageAsBitmap: Successfully downloaded, size: ${resource.width}x${resource.height}")
                            if (isCompleted.compareAndSet(false, true) && continuation.isActive) {
                                continuation.resume(resource)
                            }
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            // Called when Glide clears the target
                            Timber.d("downloadImageAsBitmap: Load cleared")
                            if (isCompleted.compareAndSet(false, true) && continuation.isActive) {
                                continuation.resume(null)
                            }
                        }
                    })
            } catch (e: Exception) {
                Timber.e(e, "Exception in downloadImageAsBitmap for URL: $url")
                if (isCompleted.compareAndSet(false, true) && continuation.isActive) {
                    continuation.resume(null)
                }
            }
            
            continuation.invokeOnCancellation {
                Timber.d("downloadImageAsBitmap: Coroutine cancelled for URL: $url")
                // Glide will handle cancellation automatically
            }
        }
    }
}


/**
 * Download and save single image from URL to storage
 * Extension for Fragment to use with lifecycleScope and access to UI features
 * @param imageUrl URL of the image to download
 * @param folder Folder path to save the image (e.g., "/${Const.APP_FOLDER}/${Const.STORY_COMIC_FOLDER}")
 * @param fileNamePrefix Prefix for the generated filename
 * @param onLoading Callback for loading state (default shows/hides fragment loading)
 * @param onSuccess Callback when download succeeds
 * @param onFailure Callback when download fails
 */
suspend fun androidx.fragment.app.Fragment.downloadAndSaveImageFromUrl(
    imageUrl: String,
    folder: String,
    fileNamePrefix: String = "downloaded_image",
    onLoading: ((Boolean) -> Unit)? = null,
    onSuccess: ((String) -> Unit)? = null,
    onFailure: ((String) -> Unit)? = null
) {
    if (imageUrl.isBlank()) {
        val message = "URL không hợp lệ"
        onFailure?.invoke(message)
        requireContext().toast(message)
        Timber.w("downloadAndSaveImageFromUrl: Empty URL")
        return
    }

    try {
        // Show loading
        onLoading?.invoke(true)

        val context = requireContext()
        Timber.d("downloadAndSaveImageFromUrl: Starting download from URL: $imageUrl")

        // Download bitmap from URL
        val bitmap = downloadImageAsBitmap(context, imageUrl)

        if (bitmap == null || bitmap.isRecycled) {
            val message = "Không thể tải ảnh"
            onLoading?.invoke(false)
            onFailure?.invoke(message)
            context.toast(message)
            Timber.e("downloadAndSaveImageFromUrl: Failed to download bitmap or bitmap is recycled")
            return
        }

        Timber.d("downloadAndSaveImageFromUrl: Downloaded bitmap successfully, size: ${bitmap.width}x${bitmap.height}")

        // Generate filename with timestamp
        val timestamp = System.currentTimeMillis()
        val fileName = "${fileNamePrefix}_${timestamp}"

        // Save to storage
        val result = com.asoft.artsal.photo.utils.StorageUtils.saveImageToStorage(
            context = context,
            pathName = folder,
            bitmap = bitmap,
            fileName = fileName
        )

        onLoading?.invoke(false)

        if (result) {
            val message = "Đã lưu ảnh thành công"
            onSuccess?.invoke("$folder/$fileName")
            context.toast(message)
            Timber.d("downloadAndSaveImageFromUrl: Saved successfully to $folder/$fileName")
        } else {
            val message = "Không thể lưu ảnh"
            onFailure?.invoke(message)
            context.toast(message)
            Timber.e("downloadAndSaveImageFromUrl: Failed to save to storage")
        }
    } catch (e: Exception) {
        onLoading?.invoke(false)
        val message = "Lỗi: ${e.message}"
        onFailure?.invoke(message)
        requireContext().toast(message)
        Timber.e(e, "Error in downloadAndSaveImageFromUrl")
    }
}

