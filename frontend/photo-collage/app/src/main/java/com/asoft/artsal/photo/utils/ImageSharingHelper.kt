package com.asoft.artsal.photo.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.asoft.artsal.photo.extensions.toast
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ImageSharingHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun shareImageToSpecificApp(image: Any, appPackage: String) {
        try {
            if (!isAppInstalled(appPackage)) {
                context.toast("App not install!")
                return
            }
            val uri = when (image) {
                is Bitmap -> image.toUri(context)
                is Uri -> image.toContentUri(context)
                is String -> Uri.parse(image).toContentUri(context)
                else -> {
                    context.toast("Error sharing image")
                    return
                }
            }
            if (uri == null) return

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                appPackage.let { `package` = it }
            }

            context.startActivity(intent)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error sharing image", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareImageToApps(context: Context, image: Any) {
        try {
            val uri = when (image) {
                is Bitmap -> image.toUri(context)
                is Uri -> image.toContentUri(context)
                is String -> Uri.parse(image).toContentUri(context)
                else -> {
                    context.toast("Error sharing image")
                    return
                }
            }
            if (uri == null) return

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Share Image"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun Bitmap.toUri(context: Context): Uri {
        val cachePath = File(context.externalCacheDir, "shared_images")
        cachePath.mkdirs()

        val file = File(cachePath, "photo_collage_${System.currentTimeMillis()}.png")
        val stream = FileOutputStream(file)

        this.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.flush()
        stream.close()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun Uri.toContentUri(context: Context): Uri? {
        return when (scheme) {
            scheme -> {
                val file = File(path ?: "")
                if (file.exists()) {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                } else {
                    return null
                }
            }

            else -> this
        }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            FirebaseEventUtils.recordException(e)
            false
        }
    }
}