package com.asoft.artsal.photo.data.model

import com.google.gson.annotations.SerializedName

data class ComicPanel(
    @SerializedName("page")
    val page: Int,

    @SerializedName("panel")
    val panel: Int,

    @SerializedName("description_vi")
    val descriptionVi: String,

    @SerializedName("description_en")
    val descriptionEn: String,

    @SerializedName("dialogue")
    val dialogue: String? = null,

    @SerializedName("speaker")
    val speaker: String? = null,

    @SerializedName("emotion")
    val emotion: String? = null
)

data class ComicImage(
    @SerializedName("key")
    val key: String,

    @SerializedName("url")
    val url: String,

    @SerializedName("presigned_url")
    val presignedUrl: String,

    @SerializedName("expires_in")
    val expiresIn: Int
)

data class ComicModel(
    @SerializedName("storyboard")
    val storyboard: String,

    @SerializedName("image")
    val image: String,

    @SerializedName("overlay")
    val overlay: String
)

data class ComicData(
    @SerializedName("comic_id")
    val comicId: String,

    @SerializedName("image")
    val image: ComicImage,

    @SerializedName("panels")
    val panels: List<ComicPanel>
)

data class ComicMeta(
    @SerializedName("pages")
    val pages: Int,

    @SerializedName("panels_per_page")
    val panelsPerPage: Int,

    @SerializedName("total_panels")
    val totalPanels: Int,

    @SerializedName("style")
    val style: String,

    @SerializedName("model")
    val model: ComicModel,

    @SerializedName("processing_time_ms")
    val processingTimeMs: Int? = null
)

data class ComicResponse(
    @SerializedName("request_id")
    val requestId: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("data")
    val data: ComicData,

    @SerializedName("meta")
    val meta: ComicMeta
)

