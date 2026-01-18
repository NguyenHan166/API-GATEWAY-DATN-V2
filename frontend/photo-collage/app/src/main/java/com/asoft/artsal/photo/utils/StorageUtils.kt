package com.asoft.artsal.photo.utils

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

object StorageUtils {
    fun saveImageToStorage(
        context: Context,
        pathName: String,
        bitmap: Bitmap,
        fileName: String
    ): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + pathName
                    )

                    // add app ownership
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                    // Set date
                    val now = System.currentTimeMillis()
                    put(MediaStore.Images.Media.DATE_ADDED, now / 1000)
                    put(MediaStore.Images.Media.DATE_MODIFIED, now / 1000)
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: return false

                // Save bitmap
                val outputStream = context.contentResolver.openOutputStream(uri)
                outputStream?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }

                // clear IS_PENDING flag
                val updateValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, updateValues, null, null)

                return true
            } else {
                // Android 9 and below - use File API
                val imagesDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    pathName
                )

                if (!imagesDir.exists()) {
                    imagesDir.mkdirs()
                }

                val imageFile = File(imagesDir, "$fileName.png")
                val outputStream = FileOutputStream(imageFile)

                outputStream.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }

                Timber.d("Image saved to file: ${imageFile.absolutePath}")
                return true
            }
        } catch (e: Exception) {
            FirebaseEventUtils.recordException(e)
            e.printStackTrace()
            false
        }
    }

    fun getImagesFromStorage(
        context: Context,
        pathName: String
    ): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ - Use MediaStore
            getImagesFromMediaStore(context, pathName)
        } else {
            // Android 9 and below - Use File API
            getImagesFromFileSystem(context, pathName)
        }
    }

    private fun getImagesFromMediaStore(
        context: Context,
        pathName: String
    ): List<String> {
        val imagesList = mutableListOf<String>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DISPLAY_NAME
        )

        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%$pathName%")

        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        )

        cursor?.use {
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

            while (it.moveToNext()) {
                val imagePath = it.getString(dataColumn)
                if (imagePath != null) {
                    imagesList.add(imagePath)
                }
            }
        }

        return imagesList
    }

    private fun getImagesFromFileSystem(
        context: Context,
        pathName: String
    ): List<String> {
        val imagesList = mutableListOf<String>()
        val imagesDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            pathName
        )

        if (imagesDir.exists() && imagesDir.isDirectory) {
            imagesDir.listFiles()?.forEach { file ->
                if (file.isFile && (file.extension.equals("png", ignoreCase = true) ||
                            file.extension.equals("jpg", ignoreCase = true) ||
                            file.extension.equals("jpeg", ignoreCase = true))
                ) {
                    imagesList.add(file.absolutePath)
                }
            }
        }

        return imagesList
    }

    fun deleteImageFromStorage(
        context: Context,
        imagePath: String
    ): DeleteResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                deleteImageFromMediaStore(context, imagePath)
            } else {
                if (deleteImageFromFileSystem(imagePath)) {
                    DeleteResult.Success
                } else {
                    DeleteResult.Error(Exception("File delete failed"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error deleting image: $imagePath")
            DeleteResult.Error(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun deleteImageFromMediaStore(
        context: Context,
        imagePath: String
    ): DeleteResult {
        val contentResolver = context.contentResolver

        try {
            val projection = arrayOf(MediaStore.Images.Media._ID)
            val selection = "${MediaStore.Images.Media.DATA} = ?"
            val selectionArgs = arrayOf(imagePath)

            val cursor = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    val deletedRows = contentResolver.delete(uri, null, null)
                    return if (deletedRows > 0) DeleteResult.Success else DeleteResult.Error(
                        Exception("Delete failed")
                    )
                } else {
                    return DeleteResult.NotFound
                }
            }
        } catch (e: RecoverableSecurityException) {
            Timber.w(e, "Need user permission to delete: $imagePath")
            return DeleteResult.RequirePermission(e.userAction.actionIntent.intentSender)
        } catch (e: Exception) {
            Timber.e(e, "Error querying MediaStore for: $imagePath")
            return DeleteResult.Error(e)
        }
        return DeleteResult.Error(Exception("Unknown error"))
    }

    private fun deleteImageFromFileSystem(imagePath: String): Boolean {
        return try {
            val file = File(imagePath)
            if (file.exists()) {
                val deleted = file.delete()
                val stillExists = file.exists()
                return deleted && !stillExists
            } else {
                return false
            }
        } catch (e: Exception) {
            false
        }
    }

    sealed class DeleteResult {
        object Success : DeleteResult()
        object NotFound : DeleteResult()
        data class RequirePermission(val intentSender: IntentSender) : DeleteResult()
        data class Error(val exception: Exception) : DeleteResult()
    }
}