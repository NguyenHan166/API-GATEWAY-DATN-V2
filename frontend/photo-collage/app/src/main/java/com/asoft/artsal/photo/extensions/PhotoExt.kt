package com.asoft.artsal.photo.extensions

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.artsal.photo.editor.collage.maker.R
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import de.hdodenhof.circleimageview.CircleImageView
import jp.wasabeef.glide.transformations.BlurTransformation
import androidx.core.net.toUri
import timber.log.Timber

fun ImageView.loadImageFromUrlWithLoading(url: String?, resize: Int? = null) {
    val circularProgressDrawable = CircularProgressDrawable(context)
    circularProgressDrawable.strokeWidth = 5f
    circularProgressDrawable.centerRadius = 30f
    circularProgressDrawable.start()
    Glide.with(this)
        .load(url)
        .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
        .placeholder(circularProgressDrawable)
        .error(R.drawable.bg_error_img)
        .into(this)
}

fun ImageView.loadBlur(url: String?) {
    Glide.with(this)
        .load(url)
        .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
        .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 6)))
        .error(R.drawable.bg_error_img)
        .into(this)
}

fun CircleImageView.loadAvatar(url: String?, resize: Int? = null) {
    Glide.with(this)
        .load(url)
        .placeholder(R.drawable.bg_rounded_grey)
        .error(R.drawable.bg_error_img)
        .into(this)
}

fun CircleImageView.loadImageDrawable(drawableRes: Int, resize: Int? = null) {
    Glide.with(this)
        .load(drawableRes)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .placeholder(R.drawable.bg_rounded_grey)
        .error(R.drawable.bg_error_img)
        .into(this)
}

fun ImageView.loadImageFromUrl(url: String?, drawableRes: Int? = null) {
    if (url.isNullOrBlank()) {
        drawableRes?.let { setImageResource(it) }
        return
    }

    val requestBuilder = Glide.with(this)
        .load(url)
        .dontAnimate()
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .timeout(10000)

    drawableRes?.let {
        requestBuilder
            .placeholder(it)
            .error(it)
    }

    // Override size nếu view chưa có kích thước
    if (width == 0 || height == 0) {
        requestBuilder.override(500, 500) // Hoặc kích thước phù hợp với app của bạn
    }

    requestBuilder.into(this)
}

fun ImageView.loadAvatarImageFromUrl(url: String?) {
    val compressFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Bitmap.CompressFormat.WEBP_LOSSY
    } else {
        Bitmap.CompressFormat.PNG
    }

    Glide.with(this)
        .load(url)
        .encodeFormat(compressFormat)
        .error(R.drawable.bg_error_img)
        .into(this)
}

fun ImageView.loadImageFromDrawable(drawableRes: Int, drawablePlaceHolder: Int? = null) {
    Glide.with(this)
        .load(drawableRes)
        .error(R.drawable.bg_error_img)
        .let { builder ->
            drawablePlaceHolder?.let {
                builder.placeholder(it)
            }
            builder
        }
        .into(this)
}

fun ImageView.loadImageGifFromDrawable(
    drawableRes: Int,
    isEnableLoop: Boolean = true,
    overrideWidth: Int? = null,
    overrideHeight: Int? = null
) {
    try {
        Glide.with(this)
            .asGif()
            .load(drawableRes)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .downsample(DownsampleStrategy.AT_MOST)
            .format(DecodeFormat.PREFER_RGB_565)
            .skipMemoryCache(false)
            .priority(Priority.LOW) // Avoid block UI
            .error(R.drawable.bg_error_img)
            .let { builder ->
                when {
                    overrideWidth != null && overrideHeight != null -> {
                        builder.override(overrideWidth, overrideHeight)
                    }

                    overrideWidth != null -> {
                        builder.override(overrideWidth, Target.SIZE_ORIGINAL)
                    }

                    overrideHeight != null -> {
                        builder.override(Target.SIZE_ORIGINAL, overrideHeight)
                    }

                    else -> {
                        builder
                    }
                }
            }
            .listener(object : RequestListener<GifDrawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<GifDrawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: GifDrawable,
                    model: Any,
                    target: Target<GifDrawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    if (!isEnableLoop) {
                        resource.setLoopCount(1)
                    }
                    return false
                }
            })
            .into(this)
    } catch (e: Exception) {
        setImageResource(R.drawable.bg_error_img)
    }
}

fun ImageView.loadCircleImageFromUrl(url: String?, drawableRes: Int) {
    Glide.with(this)
        .load(url)
        .placeholder(drawableRes)
        .error(drawableRes)
        .fitCenter()
        .circleCrop()
        .into(this)
}

fun ImageView.loadPhotoUri(
    uri: Uri?,
    colorInt: Int? = null,
    colorString: String? = null,
    requestListener: RequestListener<Drawable>? = null
) {
    colorInt?.let { background = it.toDrawable() }
    colorString?.let { background = it.toColorInt().toDrawable() }
    Glide.with(this)
        .load(uri)
        .placeholder(R.drawable.bg_rounded_grey)
        .error(R.drawable.bg_error_img)
        .addListener(requestListener)
        .into(this)
        .clearOnDetach()
}

fun View.setBackgroundFromUrl(url: String) {
    Glide.with(this).load(url)
        .into(object : CustomTarget<Drawable>() {
            override fun onResourceReady(
                resource: Drawable,
                transition: Transition<in Drawable>?
            ) {
                background = resource
            }

            override fun onLoadCleared(placeholder: Drawable?) {
            }
        })
}

fun View.loadImage(
    any: Any,
    onResourceReady: ((View, Drawable) -> Unit)? = null,
    onLoadFailed: ((View, Drawable?) -> Unit)? = null,
) {
    Glide.with(this).load(any)
        .into(object : CustomTarget<Drawable>() {
            override fun onLoadFailed(errorDrawable: Drawable?) {
                onLoadFailed?.invoke(this@loadImage, errorDrawable)
            }

            override fun onResourceReady(
                resource: Drawable,
                transition: Transition<in Drawable>?
            ) {
                onResourceReady?.invoke(this@loadImage, resource)
            }

            override fun onLoadCleared(placeholder: Drawable?) {}

        })
}

fun ImageView.loadImageDrawableWithSize(drawable: Drawable, width: Int, height: Int) {
    Glide.with(this)
        .load(drawable)
        .placeholder(R.drawable.bg_rounded_grey)
        .error(R.drawable.bg_error_img)
        .override(width, height)
        .into(this)
}

fun ImageView.loadImageDrawableWithCompress(
    drawableRes: Int,
    enableCompression: Boolean = true,
    transform: BitmapTransformation? = null,
    overrideWidth: Int? = null,
    overrideHeight: Int? = null
) {
    Glide.with(this)
        .load(drawableRes)
        .error(R.drawable.bg_error_img)
        .let { builder ->
            val compressedBuilder = if (enableCompression) {
                builder
                    .format(DecodeFormat.PREFER_RGB_565)
                    .downsample(DownsampleStrategy.CENTER_OUTSIDE)
                    .transform(transform ?: CenterCrop())
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .dontAnimate()
            } else {
                builder
            }

            when {
                overrideWidth != null && overrideHeight != null -> {
                    compressedBuilder.override(overrideWidth, overrideHeight)
                }

                overrideWidth != null -> {
                    compressedBuilder.override(overrideWidth, Target.SIZE_ORIGINAL)
                }

                overrideHeight != null -> {
                    compressedBuilder.override(Target.SIZE_ORIGINAL, overrideHeight)
                }

                else -> {
                    compressedBuilder
                }
            }
        }
        .listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>,
                isFirstResource: Boolean
            ): Boolean {
                e?.printStackTrace()
                e?.let { FirebaseEventUtils.recordException(it) }
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>?,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                return false
            }
        })
        .into(this)
}

fun ImageView.loadImageDrawableWithCompress(
    drawableRes: Drawable,
    drawablePlaceHolder: Int? = null,
    enableCompression: Boolean = true,
    transform: BitmapTransformation? = null,
    overrideWidth: Int? = null,
    overrideHeight: Int? = null
) {
    Glide.with(this)
        .load(drawableRes)
        .error(R.drawable.bg_error_img)
        .let { builder ->
            val compressedBuilder = if (enableCompression) {
                builder
                    .format(DecodeFormat.PREFER_RGB_565)
                    .downsample(DownsampleStrategy.CENTER_OUTSIDE)
                    .transform(transform ?: CenterCrop())
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .dontAnimate()
            } else {
                builder
            }

            drawablePlaceHolder?.let {
                compressedBuilder.placeholder(it)
            }

            when {
                overrideWidth != null && overrideHeight != null -> {
                    compressedBuilder.override(overrideWidth, overrideHeight)
                }

                overrideWidth != null -> {
                    compressedBuilder.override(overrideWidth, Target.SIZE_ORIGINAL)
                }

                overrideHeight != null -> {
                    compressedBuilder.override(Target.SIZE_ORIGINAL, overrideHeight)
                }

                else -> {
                    compressedBuilder
                }
            }
        }
        .listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>,
                isFirstResource: Boolean
            ): Boolean {
                e?.printStackTrace()
                e?.let { FirebaseEventUtils.recordException(it) }
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>?,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                return false
            }
        })
        .into(this)
}

fun ImageView.loadImageAssetsWithCompress(
    imagePath: String = "",
    uri: Uri? = null,
    enableCompression: Boolean = true,
    transform: BitmapTransformation? = null,
    overrideWidth: Int? = null,
    overrideHeight: Int? = null
) {
    var uriPath: Uri = imagePath.toUri()
    if (uri != null) uriPath = uri
    Glide.with(this)
        .load(uriPath)
        .error(R.drawable.bg_error_img)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .let { builder ->
            val compressedBuilder = if (enableCompression) {
                builder
                    .format(DecodeFormat.PREFER_RGB_565)
                    .downsample(DownsampleStrategy.CENTER_OUTSIDE)
                    .transform(transform ?: CenterCrop())
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .dontAnimate()
            } else {
                builder
            }

            when {
                overrideWidth != null && overrideHeight != null -> {
                    compressedBuilder.override(overrideWidth, overrideHeight)
                }

                overrideWidth != null -> {
                    compressedBuilder.override(overrideWidth, Target.SIZE_ORIGINAL)
                }

                overrideHeight != null -> {
                    compressedBuilder.override(Target.SIZE_ORIGINAL, overrideHeight)
                }

                else -> {
                    compressedBuilder
                }
            }
        }
        .listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>,
                isFirstResource: Boolean
            ): Boolean {
                e?.printStackTrace()
                e?.let { FirebaseEventUtils.recordException(it) }
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>?,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                return false
            }
        })
        .into(this)
}