package com.asoft.artsal.photo.data.model

import android.net.Uri

data class Photo(
    val id: Int,
    val timeStamp: Long,
    val timeString: String,
    val path: String,
    val uri: Uri,
    val type: PhotoType,
)

enum class PhotoType {
    Recent, Collages, Template,  AllPhoto
}
