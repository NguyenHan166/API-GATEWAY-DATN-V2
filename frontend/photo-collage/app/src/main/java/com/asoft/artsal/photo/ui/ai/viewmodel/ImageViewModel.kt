package com.asoft.artsal.photo.ui.ai.viewmodel

import android.app.Application
import android.net.Uri
import com.asoft.artsal.photo.base.BaseViewModel
import com.asoft.artsal.photo.data.model.BaseData
import com.asoft.artsal.photo.data.model.BaseResponse
import com.asoft.artsal.photo.data.model.ComicResponse
import com.asoft.artsal.photo.data.model.RelightData
import com.asoft.artsal.photo.data.model.ReplaceBgResponse
import com.asoft.artsal.photo.data.model.StoryComicRequest
import com.asoft.artsal.photo.data.model.StoryComicResponse
import com.asoft.artsal.photo.data.model.StyleTransferResponse
import com.asoft.artsal.photo.data.repository.ImageRepository
import com.asoft.artsal.photo.extensions.toMultipartBodyPart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ImageViewModel @Inject constructor(
    override val app: Application,
    private val imageRepository: ImageRepository
) : BaseViewModel(app) {

    // Upscale state

    var imageUrlToDown = ""
    private val _upscaleResult = MutableStateFlow<BaseResponse<BaseData>?>(null)
    val upscaleResult: StateFlow<BaseResponse<BaseData>?> = _upscaleResult.asStateFlow()

    private val _upscaleError = MutableStateFlow<String?>(null)
    val upscaleError: StateFlow<String?> = _upscaleError.asStateFlow()

    /**
     * Upscale image using GFPGAN
     * @param imageUri Uri of the image to upscale
     * @param scale Scale factor: 1, 2, or 4 (default: 2)
     * @param version Model version: "v1.3" or "v1.4" (default: "v1.4")
     */
    fun upscaleImage(
        imageUri: Uri,
        scale: Int? = null,
        version: String? = null
    ) {
        launchLoadingJob {
            try {
                val imagePart = imageUri.toMultipartBodyPart(app, "image")
                    ?: throw IllegalArgumentException("Failed to convert image to MultipartBody.Part")

                imageRepository.upscaleImage(imagePart, scale, version)
                    .catch { exception ->
                        Timber.e(exception, "Error upscaling image")
                        _upscaleError.value = exception.message ?: "Unknown error occurred"
                        onError.postCall(exception)
                    }
                    .collect { response ->
                        if (response.status == "success") {
                            _upscaleResult.value = response
                            _upscaleError.value = null
                            Timber.d("Upscale success: ${response.data?.url}")
                        } else {
                            _upscaleError.value = "Upscale failed: ${response.status}"
                            Timber.e("Upscale failed: ${response.status}")
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error preparing upscale request")
                _upscaleError.value = e.message ?: "Failed to prepare image"
                onError.postCall(e)
            }
        }
    }

    fun clearUpscaleResult() {
        _upscaleResult.value = null
        _upscaleError.value = null
    }

    // ==================== Portrait Relighting (IC-Light) ====================
    private val _relightResult = MutableStateFlow<BaseResponse<RelightData>?>(null)
    val relightResult: StateFlow<BaseResponse<RelightData>?> = _relightResult.asStateFlow()

    private val _relightError = MutableStateFlow<String?>(null)
    val relightError: StateFlow<String?> = _relightError.asStateFlow()

    /**
     * Relight portrait using IC-Light
     * @param imageUri Uri of the image (required)
     * @param prompt Light description (required)
     * @param lightSource "None", "Left Light", "Right Light", "Top Light", "Bottom Light" (default: "None")
     * @param appendedPrompt Additional text appended to prompt
     * @param negativePrompt Things to avoid
     * @param steps Number of inference steps (1-100)
     * @param cfg Guidance scale (1-32)
     * @param width Output width (256-1024, step 64)
     * @param height Output height (256-1024, step 64)
     * @param numberOfImages Number of output images (1-12)
     * @param outputFormat "webp", "jpg", "png"
     * @param outputQuality Output quality (1-100)
     */
    fun relightPortrait(
        imageUri: Uri,
        prompt: String,
        lightSource: String = "None"
    ) {
        launchLoadingJob {
            try {
                val imagePart = imageUri.toMultipartBodyPart(app, "image")
                    ?: throw IllegalArgumentException("Failed to convert image to MultipartBody.Part")

                Timber.d("Relight request - prompt: '$prompt', lightSource: '$lightSource'")
                Timber.d("Relight request - image part: ${imagePart.headers}, body size: ${imagePart.body.contentLength()}")

                imageRepository.relightPortrait(
                    imagePart,
                    prompt,
                    lightSource
                )
                    .catch { exception ->
                        Timber.e(exception, "Error relighting portrait")
                        _relightError.value = exception.message ?: "Unknown error occurred"
                        onError.postCall(exception)
                    }
                    .collect { response ->
                        if (response.status == "success") {
                            _relightResult.value = response
                            _relightError.value = null
                            Timber.d("Relight success: ${response.data?.outputs?.size} images")
                        } else {
                            _relightError.value = "Relight failed: ${response.status}"
                            Timber.e("Relight failed: ${response.status}")
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error preparing relight request")
                _relightError.value = e.message ?: "Failed to prepare request"
                onError.postCall(e)
            }
        }
    }

    fun clearRelightResult() {
        _relightResult.value = null
        _relightError.value = null
    }

    // ==================== Image Enhancement ====================
    private val _enhanceResult = MutableStateFlow<BaseResponse<BaseData>?>(null)
    val enhanceResult: StateFlow<BaseResponse<BaseData>?> = _enhanceResult.asStateFlow()

    private val _enhanceError = MutableStateFlow<String?>(null)
    val enhanceError: StateFlow<String?> = _enhanceError.asStateFlow()

    /**
     * Enhance image using Real-ESRGAN
     * @param imageUri Uri of the image
     * @param scale Scale factor: 2 or 4 (default: 2)
     * @param faceEnhance Enable face enhancement
     * @param model Model name (default: "real-esrgan")
     */
    fun enhanceImage(
        imageUri: Uri,
        scale: Int? = null,
        faceEnhance: Boolean? = null,
        model: String? = null
    ) {
        launchLoadingJob {
            try {
                val imagePart = imageUri.toMultipartBodyPart(app, "image")
                    ?: throw IllegalArgumentException("Failed to convert image to MultipartBody.Part")

                imageRepository.enhanceImage(imagePart, scale, faceEnhance, model)
                    .catch { exception ->
                        Timber.e(exception, "Error enhancing image")
                        _enhanceError.value = exception.message ?: "Unknown error occurred"
                        onError.postCall(exception)
                    }
                    .collect { response ->
                        if (response.status == "success") {
                            _enhanceResult.value = response
                            _enhanceError.value = null
                            Timber.d("Enhance success: ${response.data?.url}")
                        } else {
                            _enhanceError.value = "Enhance failed: ${response.status}"
                            Timber.e("Enhance failed: ${response.status}")
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error preparing enhance request")
                _enhanceError.value = e.message ?: "Failed to prepare image"
                onError.postCall(e)
            }
        }
    }

    fun clearEnhanceResult() {
        _enhanceResult.value = null
        _enhanceError.value = null
    }

    // ==================== AI Beautify ====================
    private val _beautifyResult = MutableStateFlow<BaseResponse<BaseData>?>(null)
    val beautifyResult: StateFlow<BaseResponse<BaseData>?> = _beautifyResult.asStateFlow()

    private val _beautifyError = MutableStateFlow<String?>(null)
    val beautifyError: StateFlow<String?> = _beautifyError.asStateFlow()

    /**
     * Beautify image using Real-ESRGAN
     * @param imageUri Uri of the image
     * @param scale Scale factor: 2-4 (default: 2)
     */
    fun beautifyImage(
        imageUri: Uri,
        scale: Int? = null
    ) {
        launchLoadingJob {
            try {
                val imagePart = imageUri.toMultipartBodyPart(app, "image")
                    ?: throw IllegalArgumentException("Failed to convert image to MultipartBody.Part")

                imageRepository.beautifyImage(imagePart, scale)
                    .catch { exception ->
                        Timber.e(exception, "Error beautifying image")
                        _beautifyError.value = exception.message ?: "Unknown error occurred"
                        onError.postCall(exception)
                    }
                    .collect { response ->
                        if (response.status == "success") {
                            _beautifyResult.value = response
                            _beautifyError.value = null
                            Timber.d("Beautify success: ${response.data?.url}")
                        } else {
                            _beautifyError.value = "Beautify failed: ${response.status}"
                            Timber.e("Beautify failed: ${response.status}")
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error preparing beautify request")
                _beautifyError.value = e.message ?: "Failed to prepare image"
                onError.postCall(e)
            }
        }
    }

    fun clearBeautifyResult() {
        _beautifyResult.value = null
        _beautifyError.value = null
    }

    // ==================== Background Replacement ====================
    private val _replaceBgResult = MutableStateFlow<ReplaceBgResponse?>(null)
    val replaceBgResult: StateFlow<ReplaceBgResponse?> = _replaceBgResult.asStateFlow()

    private val _replaceBgError = MutableStateFlow<String?>(null)
    val replaceBgError: StateFlow<String?> = _replaceBgError.asStateFlow()

    /**
     * Replace or remove background
     * @param fgUri Uri of foreground image (required)
     * @param bgUri Uri of background image (optional, required for replace mode)
     * @param mode "remove" or "replace" (default: "replace")
     * @param fit "cover", "contain", "fill", "inside", "outside" (default: "cover")
     * @param position "centre", "top", "bottom", "left", "right", etc. (default: "centre")
     * @param featherPx Edge smoothness (0-20, default: 1)
     * @param shadow Add shadow: "0" (no) or "1" (yes) (default: "1")
     * @param signTtl Presigned URL TTL in seconds (60-86400, default: 3600)
     */
    fun replaceBackground(
        fgUri: Uri,
        bgUri: Uri? = null,
        mode: String? = null,
        fit: String? = null,
        position: String? = null,
        featherPx: Int? = null,
        shadow: String? = null,
        signTtl: Int? = null
    ) {
        launchLoadingJob {
            try {
                val fgPart = fgUri.toMultipartBodyPart(app, "fg")
                    ?: throw IllegalArgumentException("Failed to convert foreground image to MultipartBody.Part")

                val bgPart = bgUri?.toMultipartBodyPart(app, "bg")

                // Validate mode
                if (mode == "replace" && bgPart == null) {
                    throw IllegalArgumentException("Background image is required for replace mode")
                }

                imageRepository.replaceBackground(
                    fgPart, bgPart, mode, fit, position, featherPx, shadow, signTtl
                )
                    .catch { exception ->
                        Timber.e(exception, "Error replacing background")
                        _replaceBgError.value = exception.message ?: "Unknown error occurred"
                        onError.postCall(exception)
                    }
                    .collect { response ->
//                        if (response.success == "success") {
                            _replaceBgResult.value = response
                            _replaceBgError.value = null
                            Timber.d("Replace background success: ${response.data.url}")
//                        } else {
//                            _replaceBgError.value = "Replace background failed"
//                            Timber.e("Replace background failed")
//                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error preparing replace background request")
                _replaceBgError.value = e.message ?: "Failed to prepare images"
                onError.postCall(e)
            }
        }
    }

    fun clearReplaceBgResult() {
        _replaceBgResult.value = null
        _replaceBgError.value = null
    }

    // ==================== Style Transfer ====================
    private val _styleTransferResult = MutableStateFlow<StyleTransferResponse?>(null)
    val styleTransferResult: StateFlow<StyleTransferResponse?> = _styleTransferResult.asStateFlow()

    private val _styleTransferError = MutableStateFlow<String?>(null)
    val styleTransferError: StateFlow<String?> = _styleTransferError.asStateFlow()

    /**
     * Apply style transfer to image
     * @param imageUri Uri of the image
     * @param style Style: "anime", "ghibli", "watercolor", "oil-painting", "sketches", "cartoon"
     * @param extra Additional description (e.g., "add sunset background")
     */
    fun applyStyle(
        imageUri: Uri,
        style: String,
        extra: String? = null
    ) {
        launchLoadingJob {
            try {
                val imagePart = imageUri.toMultipartBodyPart(app, "image")
                    ?: throw IllegalArgumentException("Failed to convert image to MultipartBody.Part")

                // Validate style
                val validStyles =
                    listOf("anime", "ghibli", "watercolor", "oil-painting", "sketches", "cartoon")
                if (style !in validStyles) {
                    throw IllegalArgumentException("Invalid style. Must be one of: ${validStyles.joinToString()}")
                }

                imageRepository.applyStyle(imagePart, style, extra)
                    .catch { exception ->
                        Timber.e(exception, "Error applying style")
                        _styleTransferError.value = exception.message ?: "Unknown error occurred"
                        onError.postCall(exception)
                    }
                    .collect { response ->
                        if (response.status == "success") {
                            _styleTransferResult.value = response
                            _styleTransferError.value = null
                            Timber.d("Style transfer success: ${response.data.url}")
                        } else {
                            _styleTransferError.value = "Style transfer failed: ${response.status}"
                            Timber.e("Style transfer failed with status: ${response.status}")
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error preparing style transfer request")
                _styleTransferError.value = e.message ?: "Failed to prepare image"
                onError.postCall(e)
            }
        }
    }

    fun clearStyleTransferResult() {
        _styleTransferResult.value = null
        _styleTransferError.value = null
    }

    // ==================== Comic Generation ====================
    private val _comicResult = MutableStateFlow<ComicResponse?>(null)
    val comicResult: StateFlow<ComicResponse?> = _comicResult.asStateFlow()

    private val _comicError = MutableStateFlow<String?>(null)
    val comicError: StateFlow<String?> = _comicError.asStateFlow()

    /**
     * Generate comic from text prompt
     * @param prompt Story description (≥ 5 characters)
     * @param pages Number of pages (1-3, default: 2)
     * @param panelsPerPage Number of panels per page (3-9, default: 4)
     * @param style Style: "anime", "manga", "webtoon" (default: "anime")
     */
    fun generateComic(
        prompt: String,
        pages: Int? = null,
        panelsPerPage: Int? = null,
        style: String? = null
    ) {
        launchLoadingJob {
            try {
                if (prompt.length < 5) {
                    throw IllegalArgumentException("Prompt must be at least 5 characters")
                }

                imageRepository.generateComic(prompt, pages, panelsPerPage, style)
                    .catch { exception ->
                        Timber.e(exception, "Error generating comic")
                        _comicError.value = exception.message ?: "Unknown error occurred"
                        onError.postCall(exception)
                    }
                    .collect { response ->
                        if (response.status == "success") {
                            _comicResult.value = response
                            _comicError.value = null
                            Timber.d("Comic generation success: ${response.data.comicId}")
                        } else {
                            _comicError.value = "Comic generation failed: ${response.status}"
                            Timber.e("Comic generation failed: ${response.status}")
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error preparing comic generation request")
                _comicError.value = e.message ?: "Failed to prepare request"
                onError.postCall(e)
            }
        }
    }

    fun clearComicResult() {
        _comicResult.value = null
        _comicError.value = null
    }

    // ==================== Story Comic Generation ====================
    private val _storyComicResult = MutableStateFlow<StoryComicResponse?>(null)
    val storyComicResult: StateFlow<StoryComicResponse?> = _storyComicResult.asStateFlow()

    private val _storyComicError = MutableStateFlow<String?>(null)
    val storyComicError: StateFlow<String?> = _storyComicError.asStateFlow()

    /**
     * Generate story comic (multi-page)
     * @param prompt Story description (≥ 8 characters)
     * @param pages Number of pages: 2 or 3 (default: 3)
     * @param panelsPerPage Number of panels per page: 3 or 4 (default: 4)
     * @param styleSelector Style preset: "None", "Cinematic", "Photographic", "Anime", "Manga", "Digital Art", "Pixel art", "Fantasy art", "Neonpunk", "3D Model" (default: "None")
     * @param qualitySelector Quality preset: "None", "Standard v3.0", "Standard v3.1", "Light v3.1", "Heavy v3.1" (default: "Standard v3.1")
     */
    fun generateStoryComic(
        prompt: String,
        pages: Int? = null,
        panelsPerPage: Int? = null,
        styleSelector: String? = null,
        qualitySelector: String? = null
    ) {
        launchLoadingJob {
            try {
                if (prompt.length < 8) {
                    throw IllegalArgumentException("Prompt must be at least 8 characters")
                }

                val request = StoryComicRequest(
                    prompt = prompt,
                    pages = pages,
                    panelsPerPage = panelsPerPage,
                    styleSelector = styleSelector,
                    qualitySelector = qualitySelector
                )

                imageRepository.generateStoryComic(request)
                    .catch { exception ->
                        Timber.e(exception, "Error generating story comic")
                        _storyComicError.value = exception.message ?: "Unknown error occurred"
                        onError.postCall(exception)
                    }
                    .collect { response ->
                        if (response.status == "success") {
                            _storyComicResult.value = response
                            _storyComicError.value = null
                            Timber.d("Story comic generation success: ${response.storyId}")
                        } else {
                            _storyComicError.value =
                                "Story comic generation failed: ${response.status}"
                            Timber.e("Story comic generation failed: ${response.status}")
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error preparing story comic generation request")
                _storyComicError.value = e.message ?: "Failed to prepare request"
                onError.postCall(e)
            }
        }
    }

    fun clearStoryComicResult() {
        _storyComicResult.value = null
        _storyComicError.value = null
    }
}