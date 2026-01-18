package com.asoft.artsal.photo.data.model

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody

// Request
data class RelightRequest(
    val image: MultipartBody.Part,
    val prompt: String,
    val lightSource: String = "None",
    val appendedPrompt: String? = "best quality",
    val negativePrompt: String? = "lowres, bad anatomy, bad hands, cropped, worst quality",
    val steps: Int? = 25,
    val cfg: Int? = 2,
    val width: Int? = null,
    val height: Int? = null,
    val numberOfImages: Int? = 1,
    val outputFormat: String? = "webp",
    val outputQuality: Int? = 80
)

data class RelightOutput(
    @SerializedName("url")
    val url: String,

    @SerializedName("index")
    val index: Int
)

data class RelightData(
    @SerializedName("outputs")
    val outputs: List<RelightOutput>
)

data class RelightDimensions(
    @SerializedName("width")
    val width: Int,

    @SerializedName("height")
    val height: Int
)

data class RelightMeta(
    @SerializedName("model")
    val model: String,

    @SerializedName("prompt")
    val prompt: String,

    @SerializedName("light_source")
    val lightSource: String,

    @SerializedName("steps")
    val steps: Int,

    @SerializedName("cfg")
    val cfg: Int,

    @SerializedName("dimensions")
    val dimensions: RelightDimensions,

    @SerializedName("output_format")
    val outputFormat: String,

    @SerializedName("number_of_images")
    val numberOfImages: Int
)
