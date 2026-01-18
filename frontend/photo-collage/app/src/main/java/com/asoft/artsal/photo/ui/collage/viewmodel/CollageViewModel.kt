package com.asoft.artsal.photo.ui.collage.viewmodel

import android.app.Application
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.asoft.artsal.photo.base.BaseViewModel
import com.asoft.artsal.photo.business.collage.model.PhotoItem
import com.asoft.artsal.photo.business.collage.model.TemplateItem
import com.asoft.artsal.photo.business.collage.utils.FrameImageUtils.createTemplateItems
import com.asoft.artsal.photo.data.fontTypes
import com.asoft.artsal.photo.data.model.Collage
import com.asoft.artsal.photo.data.model.FontType
import com.asoft.artsal.photo.data.model.Sticker
import com.asoft.artsal.photo.data.model.TypeView
import com.asoft.artsal.photo.extensions.internal.checkingTypeCollage
import com.asoft.artsal.photo.extensions.internal.getFontStyleByFont
import com.asoft.artsal.photo.ui.collage.adapter.CollageType
import com.asoft.artsal.photo.utils.AppUtils.colorTypes
import com.asoft.artsal.photo.utils.AssetsPackManager
import com.asoft.artsal.photo.utils.Const.ROOT_PATH
import com.asoft.artsal.photo.utils.Const.STICKER_PATH
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import com.asoft.artsal.photo.utils.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CollageViewModel @Inject constructor(
    override val app: Application,
    private val assetsPackManager: AssetsPackManager,
) : BaseViewModel(app) {
    val maxSpacing: Float = 50f
    val maxBorder: Float = 50f

    var enableSaveImage: Boolean = false
    private var photoNumberPicker = 2
    private val _frameCurrent = MutableStateFlow<TemplateItem?>(null)
    val frameCurrent: StateFlow<TemplateItem?> = _frameCurrent.asStateFlow()

    private val _images = MutableStateFlow<List<Uri>?>(null)
    val images: StateFlow<List<Uri>?> = _images.asStateFlow()
    private val _imagesMapped = MutableStateFlow<List<PhotoItem>>(emptyList())
    val imagesMapped: StateFlow<List<PhotoItem>> = _imagesMapped.asStateFlow()

    private val _viewType = MutableStateFlow<TypeView>(TypeView.Square)
    val viewType: StateFlow<TypeView> = _viewType.asStateFlow()

    val showViewHeader = SingleLiveEvent<Boolean>()

    val stickers = mutableListOf<Sticker>()

    var currentStickerNumber = 0

    var numberStickerPerRound = 0

    var currentTextNumber = 0

    init {
        val listModels = assetsPackManager.getAllModels(STICKER_PATH)
        if (listModels.isNotEmpty()) {
            initStickerFromAssets(listModels)
        }
    }

    var uriTmp: Uri? = null
    fun deleteTempFile() {
        uriTmp?.let { uri ->
            try {
                if (uri.scheme == "file") {
                    val file = File(uri.path ?: return)
                    if (file.exists()) {
                        val deleted = file.delete()
                    }
                }
                uriTmp = null
            } catch (e: Exception) {
                FirebaseEventUtils.recordException(e)
            }
        }
    }

    fun updateImageByUri(inputUri: Uri, outputUri: Uri) {
        Timber.i("updateImageByUri: inputUri=$inputUri, outputUri=$outputUri")
        val updatedImages = images.value?.map { if (it == inputUri) outputUri else it }
        setImages(updatedImages ?: emptyList())
    }

    private val _swapMode = MutableStateFlow(false)
    val swapMode: StateFlow<Boolean> = _swapMode.asStateFlow()

    private val _firstImageForSwap = MutableStateFlow<PhotoItem?>(null)
    val firstImageForSwap: StateFlow<PhotoItem?> = _firstImageForSwap.asStateFlow()

    fun enableSwapMode() {
        _swapMode.value = true
    }

    fun setFirstImageForSwap(photoItem: PhotoItem) {
        _firstImageForSwap.value = photoItem
    }

    fun clearSwapState() {
        _swapMode.value = false
        _firstImageForSwap.value = null
    }

    private fun initStickerFromAssets(imagePaths: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            imagePaths.forEach { imageName ->
                val sticker = Sticker(
                    id = UUID.randomUUID().toString().hashCode(),
                    name = imageName,
                    path = "$ROOT_PATH$STICKER_PATH/$imageName",
                )
                stickers.add(sticker)
            }
        }
    }

    fun setImages(
        images: List<Uri>,
        onImageInvalid: ((Int) -> Unit)? = null
    ) {
        if (images.size == photoNumberPicker) {
            onImageInvalid?.invoke(1)
        } else {
            onImageInvalid?.invoke(photoNumberPicker)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _images.value = images
        }
    }

    private val _text = MutableStateFlow<String>("")
    val text: StateFlow<String> = _text.asStateFlow()
    fun setText(text: String) {
        if (text.isNotEmpty()) {
            _text.value = text
        }
    }

    private val _textStyle = MutableStateFlow<TextStyle>(TextStyle())
    val textStyle: StateFlow<TextStyle> = _textStyle.asStateFlow()

    fun getTextStyle() = textStyle.value

    fun setTextColor(color: Int) {
        _textStyle.update { it.copy(textColor = color) }
    }

    fun setFontStyle(fontStyle: FontType?) {
        if (fontStyle == null) {
            return
        }
        _textStyle.update { it.copy(fontStyle = fontStyle) }
    }

    fun setFontStyle(font: Int?) {
        if (font == null || font == -1) {
            return
        }
        _textStyle.update { it.copy(fontStyle = font.getFontStyleByFont()) }
    }

    fun resetText() {
        _text.value = ""
        _textStyle.value = TextStyle()
    }

    private val _border = MutableStateFlow<Border>(Border())
    val border: StateFlow<Border> = _border.asStateFlow()

    fun getBorder(): Border {
        return border.value
    }

    fun updateBorderOuter(borderOuter: Float) {
        updateBorder(border.value.copy(borderOuter = borderOuter))
    }

    fun updateBorderInner(borderInner: Float) {
        updateBorder(border.value.copy(borderInner = borderInner))
    }

    fun updateBorderRadius(borderRadius: Float) {
        updateBorder(border.value.copy(borderRadius = borderRadius))
    }

    fun updateBorder(border: Border) {
        viewModelScope.launch(Dispatchers.IO) {
            _border.emit(border)
        }
    }

    private val _backgroundColor = MutableStateFlow<Int>(Color.WHITE)
    val backgroundColor: StateFlow<Int> = _backgroundColor.asStateFlow()
    fun getBackgroundColor() = backgroundColor.value

    fun updateBackgroundColor(color: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _backgroundColor.emit(color)
        }
    }

    private fun updateFrameCurrent(templateItem: TemplateItem?) {
        if (templateItem != null) _frameCurrent.value = templateItem
    }

    private fun updatePhotoNumbersLimit(collageName: String) {
        val photoNumbersLimit = when (collageName.checkingTypeCollage()) {
            CollageType.Collage2 -> 2
            CollageType.Collage3 -> 3
            CollageType.Collage4 -> 4
            else -> 5
        }
        photoNumberPicker = photoNumbersLimit
    }

    fun updateDataCollage(collage: Collage) {
        _viewType.value = collage.typeView
        updatePhotoNumbersLimit(collage.collageName)
        updateFrameCurrent(createTemplateItems(collage.collageName))
    }

    fun updateImageMapped(images: List<Uri>) {
        if (images.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _imagesMapped.value = frameCurrent.value?.photoItemList?.mapIndexed { index, item ->
                item.apply {
                    runCatching {
                        imagePath = images[index]
                    }
                }
            } ?: emptyList()
        }
    }

    fun clearAllData() {
        _images.value = null
        _imagesMapped.value = emptyList()
        _frameCurrent.value = null
        _border.value = Border()
        _backgroundColor.value = Color.WHITE
        photoNumberPicker = 2
        enableSaveImage = false
        currentStickerNumber = 0
        currentTextNumber = 0
        numberStickerPerRound = 0
        clearSwapState()
        resetText()
        deleteTempFile()
    }
    fun clearDataTmp() {
        _border.value = Border()
        _backgroundColor.value = Color.WHITE
        photoNumberPicker = 2
        enableSaveImage = false
        currentStickerNumber = 0
        currentTextNumber = 0
        numberStickerPerRound = 0
        clearSwapState()
        deleteTempFile()
    }
    fun clearOnDestroyView() {
        enableSaveImage = false
        currentStickerNumber = 0
        currentTextNumber = 0
        numberStickerPerRound = 0
        clearSwapState()
    }
}

data class Border(
    val borderOuter: Float = 0f, val borderInner: Float = 0f, val borderRadius: Float = 0f
)

data class TextStyle(
    val textColor: Int = colorTypes[0],
    val fontStyle: FontType = fontTypes[0],
)
