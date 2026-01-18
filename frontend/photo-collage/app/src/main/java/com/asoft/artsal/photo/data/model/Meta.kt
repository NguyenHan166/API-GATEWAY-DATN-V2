package com.asoft.artsal.photo.data.model

import com.google.gson.annotations.SerializedName

data class Size(
    @SerializedName("width")
    val width: Int,

    @SerializedName("height")
    val height: Int
)

data class Meta(
    @SerializedName("model")
    val model: String? = null,

    @SerializedName("version")
    val version: String? = null,

    @SerializedName("scale")
    val scale: Int? = null,

    @SerializedName("input_size")
    val inputSize: Size? = null,

    @SerializedName("output_size")
    val outputSize: Size? = null,

    // Legacy fields for backward compatibility
    @SerializedName("bytes")
    val bytes: Int? = null,

    @SerializedName("requestId")
    val requestId: String? = null
)