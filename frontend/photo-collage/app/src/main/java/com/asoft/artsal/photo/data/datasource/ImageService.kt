package com.asoft.artsal.photo.data.datasource

import okhttp3.MultipartBody
import com.asoft.artsal.photo.data.model.BaseData
import com.asoft.artsal.photo.data.model.BaseResponse
import com.asoft.artsal.photo.data.model.ComicResponse
import com.asoft.artsal.photo.data.model.RelightData
import com.asoft.artsal.photo.data.model.ReplaceBgResponse
import com.asoft.artsal.photo.data.model.StoryComicRequest
import com.asoft.artsal.photo.data.model.StoryComicResponse
import com.asoft.artsal.photo.data.model.StyleTransferResponse
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ImageService {
    // Endpoint: POST /upscale
    @Multipart
    @POST("api/upscale")
    suspend fun upscaleImage(
        @Part image: MultipartBody.Part,
        @Part scale: MultipartBody.Part,
        @Part version: MultipartBody.Part
    ): BaseResponse<BaseData>

    @Multipart
    @POST("api/portraits/ic-light")
    suspend fun relightPortrait(
        @Part image: MultipartBody.Part,
        @Part prompt: MultipartBody.Part,
        @Part lightSource: MultipartBody.Part
    ): BaseResponse<RelightData>

    @Multipart
    @POST("api/clarity")
    suspend fun improveClarity(
        @Part image: MultipartBody.Part,
        @Part scale: MultipartBody.Part,
        @Part faceEnhance: MultipartBody.Part
    ): BaseResponse<BaseData>

    // Endpoint: POST /enhance
    @Multipart
    @POST("api/enhance")
    suspend fun enhanceImage(
        @Part image: MultipartBody.Part,
        @Part scale: MultipartBody.Part,
        @Part faceEnhance: MultipartBody.Part,
        @Part model: MultipartBody.Part
    ): BaseResponse<BaseData>

    // Endpoint: POST /ai-beautify
    @Multipart
    @POST("api/ai-beautify")
    suspend fun beautifyImage(
        @Part image: MultipartBody.Part,
        @Part scale: MultipartBody.Part
    ): BaseResponse<BaseData>

    // Endpoint: POST /replace-bg
    @Multipart
    @POST("api/replace-bg")
    suspend fun replaceBackground(
        @Part fg: MultipartBody.Part,
        @Part bg: MultipartBody.Part,
        @Part mode: MultipartBody.Part,
        @Part fit: MultipartBody.Part,
        @Part position: MultipartBody.Part,
        @Part featherPx: MultipartBody.Part,
        @Part shadow: MultipartBody.Part,
        @Part signTtl: MultipartBody.Part
    ): ReplaceBgResponse

    // Endpoint: POST /style/replace-style
    @Multipart
    @POST("api/style")
    suspend fun applyStyle(
        @Part image: MultipartBody.Part,
        @Part style: MultipartBody.Part,
        @Part extra: MultipartBody.Part
    ): StyleTransferResponse

    // Endpoint: POST /comic/generate
    @Multipart
    @POST("api/comic/generate")
    suspend fun generateComic(
        @Part prompt: MultipartBody.Part,
        @Part pages: MultipartBody.Part,
        @Part panelsPerPage: MultipartBody.Part,
        @Part style: MultipartBody.Part
    ): ComicResponse

    // Endpoint: POST /story-comic/generate
    @POST("api/story-comic/generate")
    suspend fun generateStoryComic(
        @Body request: StoryComicRequest
    ): StoryComicResponse

}