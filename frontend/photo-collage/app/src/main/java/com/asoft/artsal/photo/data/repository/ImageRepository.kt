package com.asoft.artsal.photo.data.repository

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.asoft.artsal.photo.data.datasource.ImageService
import com.asoft.artsal.photo.data.model.BaseData
import com.asoft.artsal.photo.data.model.BaseResponse
import com.asoft.artsal.photo.data.model.ComicResponse
import com.asoft.artsal.photo.data.model.RelightData
import com.asoft.artsal.photo.data.model.ReplaceBgResponse
import com.asoft.artsal.photo.data.model.StoryComicRequest
import com.asoft.artsal.photo.data.model.StoryComicResponse
import com.asoft.artsal.photo.data.model.StyleTransferResponse
import com.asoft.artsal.photo.extensions.toMultipartBodyPart
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ImageRepositoryImpl @Inject constructor(
    private val imageService: ImageService
) : ImageRepository {
    
    override suspend fun upscaleImage(
        imagePart: MultipartBody.Part,
        scale: Int?,
        version: String?
    ): Flow<BaseResponse<BaseData>> = flow {
        emit(imageService.upscaleImage(
            imagePart,
            scale.toMultipartBodyPart("scale"),
            version.toMultipartBodyPart("version")
        ))
    }.flowOn(Dispatchers.IO)

    override suspend fun relightPortrait(
        imagePart: MultipartBody.Part,
        prompt: String,
        lightSource: String
    ): Flow<BaseResponse<RelightData>> = flow {
        // Send required parameters only: image, prompt, light_source
        val promptPart = MultipartBody.Part.createFormData("prompt", prompt)
        val lightSourcePart = MultipartBody.Part.createFormData("light_source", lightSource)

        emit(
            imageService.relightPortrait(
                imagePart,
                promptPart,
                lightSourcePart
            )
        )
    }.flowOn(Dispatchers.IO)

    override suspend fun improveClarity(
        imagePart: MultipartBody.Part,
        scale: Int?,
        faceEnhance: Boolean?
    ): Flow<BaseResponse<BaseData>> = flow {
        emit(imageService.improveClarity(
            imagePart,
            scale.toMultipartBodyPart("scale"),
            faceEnhance.toMultipartBodyPart("faceEnhance")
        ))
    }.flowOn(Dispatchers.IO)

    override suspend fun enhanceImage(
        imagePart: MultipartBody.Part,
        scale: Int?,
        faceEnhance: Boolean?,
        model: String?
    ): Flow<BaseResponse<BaseData>> = flow {
        emit(imageService.enhanceImage(
            imagePart,
            scale.toMultipartBodyPart("scale"),
            faceEnhance.toMultipartBodyPart("face_enhance"),
            model.toMultipartBodyPart("model")
        ))
    }.flowOn(Dispatchers.IO)

    override suspend fun beautifyImage(
        imagePart: MultipartBody.Part,
        scale: Int?
    ): Flow<BaseResponse<BaseData>> = flow {
        emit(imageService.beautifyImage(
            imagePart,
            scale.toMultipartBodyPart("scale")
        ))
    }.flowOn(Dispatchers.IO)

    override suspend fun replaceBackground(
        fgPart: MultipartBody.Part,
        bgPart: MultipartBody.Part?,
        mode: String?,
        fit: String?,
        position: String?,
        featherPx: Int?,
        shadow: String?,
        signTtl: Int?
    ): Flow<ReplaceBgResponse> = flow {
        // Create empty part if bgPart is null
        val bgPartNonNull = bgPart ?: run {
            val emptyBody = "".toRequestBody("text/plain".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("bg", null, emptyBody)
        }
        
        emit(imageService.replaceBackground(
            fgPart,
            bgPartNonNull,
            mode.toMultipartBodyPart("mode"),
            fit.toMultipartBodyPart("fit"),
            position.toMultipartBodyPart("position"),
            featherPx.toMultipartBodyPart("featherPx"),
            shadow.toMultipartBodyPart("shadow"),
            signTtl.toMultipartBodyPart("signTtl")
        ))
    }.flowOn(Dispatchers.IO)

    override suspend fun applyStyle(
        imagePart: MultipartBody.Part,
        style: String,
        extra: String?
    ): Flow<StyleTransferResponse> = flow {
        emit(imageService.applyStyle(
            imagePart,
            style.toMultipartBodyPart("style"),
            extra.toMultipartBodyPart("extra")
        ))
    }.flowOn(Dispatchers.IO)

    override suspend fun generateComic(
        prompt: String,
        pages: Int?,
        panelsPerPage: Int?,
        style: String?
    ): Flow<ComicResponse> = flow {
        emit(imageService.generateComic(
            prompt.toMultipartBodyPart("prompt"),
            pages.toMultipartBodyPart("pages"),
            panelsPerPage.toMultipartBodyPart("panelsPerPage"),
            style.toMultipartBodyPart("style")
        ))
    }.flowOn(Dispatchers.IO)

    override suspend fun generateStoryComic(
        request: StoryComicRequest
    ): Flow<StoryComicResponse> = flow {
        emit(imageService.generateStoryComic(request))
    }.flowOn(Dispatchers.IO)

}

interface ImageRepository {
    suspend fun upscaleImage(
        imagePart: MultipartBody.Part,
        scale: Int? = null,
        version: String? = null
    ): Flow<BaseResponse<BaseData>>

    suspend fun relightPortrait(
        imagePart: MultipartBody.Part,
        prompt: String,
        lightSource: String = "None"
    ): Flow<BaseResponse<RelightData>>

    suspend fun improveClarity(
        imagePart: MultipartBody.Part,
        scale: Int? = null,
        faceEnhance: Boolean? = null
    ): Flow<BaseResponse<BaseData>>

    suspend fun enhanceImage(
        imagePart: MultipartBody.Part,
        scale: Int? = null,
        faceEnhance: Boolean? = null,
        model: String? = null
    ): Flow<BaseResponse<BaseData>>

    suspend fun beautifyImage(
        imagePart: MultipartBody.Part,
        scale: Int? = null
    ): Flow<BaseResponse<BaseData>>

    suspend fun replaceBackground(
        fgPart: MultipartBody.Part,
        bgPart: MultipartBody.Part? = null,
        mode: String? = null,
        fit: String? = null,
        position: String? = null,
        featherPx: Int? = null,
        shadow: String? = null,
        signTtl: Int? = null
    ): Flow<ReplaceBgResponse>

    suspend fun applyStyle(
        imagePart: MultipartBody.Part,
        style: String,
        extra: String? = null
    ): Flow<StyleTransferResponse>

    suspend fun generateComic(
        prompt: String,
        pages: Int? = null,
        panelsPerPage: Int? = null,
        style: String? = null
    ): Flow<ComicResponse>

    suspend fun generateStoryComic(
        request: StoryComicRequest
    ): Flow<StoryComicResponse>
}