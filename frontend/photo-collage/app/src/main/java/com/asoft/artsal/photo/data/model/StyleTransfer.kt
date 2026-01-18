package com.asoft.artsal.photo.data.model

import com.google.gson.annotations.SerializedName

data class StyleTransferMeta(
    @SerializedName("style")
    val style: String,

    @SerializedName("bytes")
    val bytes: Int? = null,

    @SerializedName("requestId")
    val requestId: String? = null,

    @SerializedName("request_id")
    val requestIdSnake: String? = null
)

data class StyleTransferData(
    @SerializedName("key")
    val key: String,

    @SerializedName("url")
    val url: String,

    @SerializedName("presigned_url")
    val presignedUrl: String? = null,

    @SerializedName("expires_in")
    val expiresIn: Int? = null,

    @SerializedName("meta")
    val meta: StyleTransferMeta? = null
)

data class StyleTransferResponse(
    @SerializedName("request_id")
    val requestId: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("data")
    val data: StyleTransferData,

    @SerializedName("meta")
    val meta: StyleTransferMeta? = null
) {
    // Helper property to check if response is successful
    val success: Boolean
        get() = status == "success"
}

