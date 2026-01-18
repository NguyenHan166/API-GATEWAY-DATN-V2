package com.asoft.artsal.photo.data.model

import com.google.gson.annotations.SerializedName

data class BaseResponse<T>(
    @SerializedName("request_id")
    val requestId: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("meta")
    val meta: Meta? = null,

    @SerializedName("data")
    val data: T? = null
)