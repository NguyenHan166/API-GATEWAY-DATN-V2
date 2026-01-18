package com.asoft.artsal.photo.data.model

import com.google.gson.annotations.SerializedName

open class BaseData(
    @SerializedName("key")
    val key: String,

    @SerializedName("url")
    val url: String,

    @SerializedName("presigned_url")
    val presignedUrl: String,

    @SerializedName("expires_in")
    val expiresIn: Int
)
