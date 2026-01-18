package com.asoft.artsal.photo.data.model

import com.google.gson.annotations.SerializedName

data class ReplaceBgMeta(
    @SerializedName("width")
    val width: Int,

    @SerializedName("height")
    val height: Int,

    @SerializedName("mode")
    val mode: String
)

data class ReplaceBgData(
    @SerializedName("key")
    val key: String,

    @SerializedName("url")
    val url: String,

    @SerializedName("presignedUrl")
    val presignedUrl: String,

    @SerializedName("expiresIn")
    val expiresIn: Int,

    @SerializedName("meta")
    val meta: ReplaceBgMeta? = null
)

data class ReplaceBgResponse(
    @SerializedName("success")
    val success: String,

    @SerializedName("requestId")
    val requestId: String,

    @SerializedName("data")
    val data: ReplaceBgData
)

