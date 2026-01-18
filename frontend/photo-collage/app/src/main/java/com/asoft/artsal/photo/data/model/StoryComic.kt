package com.asoft.artsal.photo.data.model

import com.google.gson.annotations.SerializedName

data class StoryComicPanel(
    @SerializedName("id")
    val id: Int,

    @SerializedName("dialogue")
    val dialogue: String? = null,

    @SerializedName("speaker")
    val speaker: String? = null,

    @SerializedName("emotion")
    val emotion: String? = null
)

data class StoryComicPage(
    @SerializedName("page_index")
    val pageIndex: Int,

    @SerializedName("page_url")
    val pageUrl: String,

    @SerializedName("key")
    val key: String,

    @SerializedName("presigned_url")
    val presignedUrl: String? = null,

    @SerializedName("panels")
    val panels: List<StoryComicPanel>
)

data class StoryComicOutline(
    @SerializedName("id")
    val id: Int,

    @SerializedName("summary_vi")
    val summaryVi: String,

    @SerializedName("main_emotion")
    val mainEmotion: String
)

data class StoryComicPageMeta(
    @SerializedName("page_index")
    val pageIndex: Int,

    @SerializedName("beats")
    val beats: List<Int>,

    @SerializedName("panel_count")
    val panelCount: Int
)

data class StoryComicModel(
    @SerializedName("llm")
    val llm: String,

    @SerializedName("image")
    val image: String,

    @SerializedName("style_selector")
    val styleSelector: String? = null,

    @SerializedName("quality_selector")
    val qualitySelector: String? = null
)

data class StoryComicMeta(
    @SerializedName("outline")
    val outline: List<StoryComicOutline>,

    @SerializedName("pages")
    val pages: List<StoryComicPageMeta>,

    @SerializedName("model")
    val model: StoryComicModel
)

data class StoryComicResponse(
    @SerializedName("request_id")
    val requestId: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("story_id")
    val storyId: String? = null,

    @SerializedName("pages")
    val pages: List<StoryComicPage>,

    @SerializedName("meta")
    val meta: StoryComicMeta
)

