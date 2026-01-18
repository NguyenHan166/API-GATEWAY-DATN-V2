package com.asoft.artsal.photo.data.model

import com.google.gson.annotations.SerializedName

// Data classes cho response
data class EnhanceData(
    @SerializedName("key")
    val key: String,

    @SerializedName("url")
    val url: String,

    @SerializedName("presigned_url")
    val presignedUrl: String,

    @SerializedName("expires_in")
    val expiresIn: Int
)

data class EnhanceMeta(
    @SerializedName("model")
    val model: String,

    @SerializedName("scale")
    val scale: Int,

    @SerializedName("faceEnhance")
    val faceEnhance: Boolean
)

data class EnhanceResponse(
    @SerializedName("request_id")
    val requestId: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("data")
    val data: EnhanceData,

    @SerializedName("meta")
    val meta: EnhanceMeta
)