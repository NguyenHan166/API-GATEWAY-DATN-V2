package com.asoft.artsal.photo.extensions

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.MimeTypeMap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.MediaType.Companion.toMediaTypeOrNull as okHttp3ToMediaTypeOrNull
import okhttp3.RequestBody as OkHttp3RequestBody
import okhttp3.RequestBody.Companion.toRequestBody as okHttp3ToRequestBody
import java.io.File
import java.io.Serializable

fun String.toRequestBodyPart(): OkHttp3RequestBody = this.okHttp3ToRequestBody(this.okHttp3ToMediaTypeOrNull())

/**
 * Convert Int to MultipartBody.Part for form data
 * Returns empty part if value is null (to avoid Retrofit nullable Part issue)
 */
fun Int?.toMultipartBodyPart(partName: String): MultipartBody.Part {
    return if (this != null) {
        val requestBody = this.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        MultipartBody.Part.createFormData(partName, null, requestBody)
    } else {
        // Return empty part to avoid null issue with Retrofit
        val emptyBody = "".toRequestBody("text/plain".toMediaTypeOrNull())
        MultipartBody.Part.createFormData(partName, null, emptyBody)
    }
}

/**
 * Convert String to MultipartBody.Part for form data
 * Returns empty part if value is null (to avoid Retrofit nullable Part issue)
 * For form-data text fields, we don't set Content-Type to match Postman behavior
 */
fun String?.toMultipartBodyPart(partName: String): MultipartBody.Part {
    return if (this != null && this.isNotBlank()) {
        // Don't set Content-Type for form-data text fields (matches Postman behavior)
        val requestBody = this.toRequestBody(null) // null = no Content-Type header
        MultipartBody.Part.createFormData(partName, null, requestBody)
    } else {
        // Return empty part to avoid null issue with Retrofit
        val emptyBody = "".toRequestBody(null)
        MultipartBody.Part.createFormData(partName, null, emptyBody)
    }
}

/**
 * Convert Boolean to MultipartBody.Part for form data
 * Returns empty part if value is null (to avoid Retrofit nullable Part issue)
 */
fun Boolean?.toMultipartBodyPart(partName: String): MultipartBody.Part {
    return if (this != null) {
        val requestBody = this.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        MultipartBody.Part.createFormData(partName, null, requestBody)
    } else {
        // Return empty part to avoid null issue with Retrofit
        val emptyBody = "".toRequestBody("text/plain".toMediaTypeOrNull())
        MultipartBody.Part.createFormData(partName, null, emptyBody)
    }
}

/**
 * Convert Uri to MultipartBody.Part for image upload
 * Determines MIME type from Uri and ensures proper binary data is sent
 */
fun Uri.toMultipartBodyPart(context: Context, partName: String = "image"): MultipartBody.Part? {
    return try {
        val inputStream = context.contentResolver.openInputStream(this) ?: return null
        
        // Try to get MIME type from ContentResolver first (most reliable)
        var mimeType = context.contentResolver.getType(this)
        
        // If not available, try to extract from Uri path
        if (mimeType == null) {
            val uriString = this.toString()
            val lastDot = uriString.lastIndexOf('.')
            if (lastDot != -1 && lastDot < uriString.length - 1) {
                val extension = uriString.substring(lastDot + 1).lowercase()
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            }
        }
        
        // Default to jpeg if cannot determine
        mimeType = mimeType ?: "image/jpeg"
        
        // Ensure it's an image MIME type
        val imageMimeType = if (mimeType.startsWith("image/")) {
            mimeType
        } else {
            "image/jpeg" // Fallback to jpeg
        }
        
        // Create temp file with appropriate extension based on MIME type
        val extension = when {
            imageMimeType.contains("jpeg") || imageMimeType.contains("jpg") -> ".jpg"
            imageMimeType.contains("png") -> ".png"
            imageMimeType.contains("webp") -> ".webp"
            imageMimeType.contains("gif") -> ".gif"
            else -> ".jpg"
        }
        
        val file = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}$extension")
        
        // Copy input stream to file (binary data)
        inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        // Verify file was created and has content
        if (!file.exists() || file.length() == 0L) {
            return null
        }
        
        // Create RequestBody with correct MIME type for image (NOT application/json!)
        val requestFile = file.asRequestBody(imageMimeType.toMediaTypeOrNull())
        
        // Create MultipartBody.Part with correct part name and filename
        // This ensures the server receives the file with proper Content-Type: image/jpeg (or image/png, etc.)
        MultipartBody.Part.createFormData(partName, file.name, requestFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Convert File to MultipartBody.Part for image upload
 * Determines MIME type from file extension
 */
fun File.toMultipartBodyPart(partName: String = "image"): MultipartBody.Part? {
    return try {
        if (!exists() || length() == 0L) return null
        
        // Determine MIME type from file extension
        val extension = extension.lowercase()
        val mimeType = when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) 
                ?: "image/jpeg" // Default to jpeg
        }
        
        // Ensure it's an image MIME type
        val imageMimeType = if (mimeType.startsWith("image/")) {
            mimeType
        } else {
            "image/jpeg" // Fallback to jpeg
        }
        
        // Create RequestBody with correct MIME type for image
        val requestFile = asRequestBody(imageMimeType.toMediaTypeOrNull())
        
        // Create MultipartBody.Part with correct part name and filename
        MultipartBody.Part.createFormData(partName, name, requestFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

inline fun <reified T : Any> Any.mapTo(): T =
    GsonBuilder().create().run {
        fromJson(toJson(this@mapTo), T::class.java)
    }

inline fun <reified T : Serializable> Bundle.serializable(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getSerializable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializable(key) as? T
}


inline fun <reified T> String.mapMQTT(): T? = Gson().fromJson(this, T::class.java)