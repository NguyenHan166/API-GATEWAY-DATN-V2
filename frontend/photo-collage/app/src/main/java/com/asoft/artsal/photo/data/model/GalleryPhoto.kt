package com.asoft.artsal.photo.data.model

import android.net.Uri
import android.os.Parcelable
import java.io.Serializable

data class GalleryPhoto(
    val uri: Uri,
    val isSelected: Boolean = false
) : Serializable
