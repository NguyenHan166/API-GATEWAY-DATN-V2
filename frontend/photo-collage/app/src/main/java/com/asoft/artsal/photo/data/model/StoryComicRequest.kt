package com.asoft.artsal.photo.data.model

import com.google.gson.annotations.SerializedName

data class StoryComicRequest(
    @SerializedName("prompt")
    val prompt: String,

    @SerializedName("pages")
    val pages: Int? = null,

    @SerializedName("panels_per_page")
    val panelsPerPage: Int? = null,

    @SerializedName("style_selector")
    val styleSelector: String? = null,

    @SerializedName("quality_selector")
    val qualitySelector: String? = null
)

