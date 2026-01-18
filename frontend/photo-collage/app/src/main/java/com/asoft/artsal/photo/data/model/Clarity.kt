package com.asoft.artsal.photo.data.model

import com.google.gson.annotations.SerializedName

// Data classes cho response
data class ClarityData(
    @SerializedName("key")
    val key: String,

    @SerializedName("url")
    val url: String,

    @SerializedName("presigned_url")
    val presignedUrl: String,

    @SerializedName("expires_in")
    val expiresIn: Int
)

data class ImageSize(
    @SerializedName("width")
    val width: Int,

    @SerializedName("height")
    val height: Int
)

data class ClarityMeta(
    @SerializedName("model")
    val model: String,

    @SerializedName("scale")
    val scale: Int,

    @SerializedName("face_enhance")
    val faceEnhance: Boolean,

    @SerializedName("input_size")
    val inputSize: ImageSize,

    @SerializedName("output_size")
    val outputSize: ImageSize
)

data class ClarityResponse(
    @SerializedName("request_id")
    val requestId: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("data")
    val data: ClarityData,

    @SerializedName("meta")
    val meta: ClarityMeta
)