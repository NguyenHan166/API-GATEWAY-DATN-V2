package com.asoft.artsal.photo

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.artsal.photo.editor.collage.maker.BuildConfig
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions

@GlideModule
class GlideApplication : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // Disk cache configuration
        val diskCacheSizeBytes = 1024 * 1024 * 500 // 500 MB
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, diskCacheSizeBytes.toLong()))

        // Memory management based on device capabilities
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryClass = activityManager?.memoryClass ?: 16
        val isLowRamDevice = activityManager?.isLowRamDevice == true

        // Memory cache configuration
        val memoryCacheSize = if (isLowRamDevice) {
            // Low RAM devices: 15MB
            1024 * 1024 * 15
        } else {
            // Normal devices: Use 1/8 of available memory, max 50MB
            val maxSize = 1024 * 1024 * 50 // 50MB max
            val calculatedSize = 1024 * 1024 * memoryClass / 8
            minOf(maxSize, calculatedSize)
        }
        builder.setMemoryCache(LruResourceCache(memoryCacheSize.toLong()))

        // Bitmap pool configuration
        val bitmapPoolSize = if (isLowRamDevice) {
            // Low RAM devices: 10MB
            1024 * 1024 * 10
        } else {
            // Normal devices: 30MB
            1024 * 1024 * 30
        }
        builder.setBitmapPool(LruBitmapPool(bitmapPoolSize.toLong()))

        // Default request options based on device capability
        val defaultOptions = if (isLowRamDevice) {
            RequestOptions()
                .format(DecodeFormat.PREFER_RGB_565)
                .downsample(DownsampleStrategy.AT_MOST)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .skipMemoryCache(false)
        } else {
            RequestOptions()
                .format(DecodeFormat.PREFER_ARGB_8888)
                .downsample(DownsampleStrategy.CENTER_OUTSIDE)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .skipMemoryCache(false)
        }

        builder.setDefaultRequestOptions(defaultOptions)

        // Log level for debugging (remove in production)
        if (BuildConfig.DEBUG) {
            builder.setLogLevel(Log.DEBUG)
        }
    }

    override fun isManifestParsingEnabled(): Boolean {
        // Disable manifest parsing for better performance
        return false
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        super.registerComponents(context, glide, registry)
    }
}