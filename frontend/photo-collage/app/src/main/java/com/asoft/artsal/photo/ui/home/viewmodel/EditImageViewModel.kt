package com.asoft.artsal.photo.ui.home.viewmodel

import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import coil3.Bitmap
import com.asoft.artsal.photo.base.BaseViewModel
import com.asoft.artsal.photo.data.model.Filter
import com.asoft.artsal.photo.data.model.FilterType
import com.asoft.artsal.photo.extensions.internal.findByFilterType
import com.asoft.artsal.photo.utils.AssetsPackManager
import com.asoft.artsal.photo.utils.Const.BAW_PATH
import com.asoft.artsal.photo.utils.Const.CINEMATIC_PATH
import com.asoft.artsal.photo.utils.Const.FILTER_PATH
import com.asoft.artsal.photo.utils.Const.LANDSCAPE_PATH
import com.asoft.artsal.photo.utils.Const.LIFESTYLE_PATH
import com.asoft.artsal.photo.utils.Const.MOODY_PATH
import com.asoft.artsal.photo.utils.Const.NATURE_PATH
import com.asoft.artsal.photo.utils.Const.PORTRAIT_PATH
import com.asoft.artsal.photo.utils.Const.ROOT_PATH
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import com.asoft.artsal.photo.utils.SharePreference
import com.asoft.artsal.photo.utils.SharePreference.Companion.IS_ENABLE_SHOW_RATE
import com.asoft.artsal.photo.utils.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditImageViewModel @Inject constructor(
    override val app: Application,
    private val assetsPackManager: AssetsPackManager,
    private val sharePreference: SharePreference
) : BaseViewModel(app) {
    val callCropSuccess = SingleLiveEvent<Boolean>()
    var isTemplateFunction = false
    val filterBAW = mutableListOf<Filter>()
    val filterLandscape = mutableListOf<Filter>()
    val filterLifeStyle = mutableListOf<Filter>()
    val filterMoody = mutableListOf<Filter>()
    val filterNature = mutableListOf<Filter>()
    val filterPortrait = mutableListOf<Filter>()
    val filterCinematic = mutableListOf<Filter>()
    var lutPaths = listOf<String>()



    init {
        Timber.i("setup filter image from assets...")
        val bawPaths = assetsPackManager.getAllModels(FILTER_PATH + BAW_PATH)
        val cinematicPaths = assetsPackManager.getAllModels(FILTER_PATH + CINEMATIC_PATH)
        val landscapePaths = assetsPackManager.getAllModels(FILTER_PATH + LANDSCAPE_PATH)
        val lifeStylePaths = assetsPackManager.getAllModels(FILTER_PATH + LIFESTYLE_PATH)
        val moodyPaths = assetsPackManager.getAllModels(FILTER_PATH + MOODY_PATH)
        val naturalPaths = assetsPackManager.getAllModels(FILTER_PATH + NATURE_PATH)
        val portraitPaths = assetsPackManager.getAllModels(FILTER_PATH + PORTRAIT_PATH)

        viewModelScope.launch(Dispatchers.IO) {
            val lutPathsDeferred = async {
                assetsPackManager.copyLutFilesToInternalStorage()
            }
            lutPaths = lutPathsDeferred.await()
            Timber.i("setup filter image from assets...$lutPaths")

            launch {
                setupFilterFromAssets(bawPaths, FilterType.BAndW)
                setupFilterFromAssets(cinematicPaths, FilterType.Cinematic)
                setupFilterFromAssets(landscapePaths, FilterType.Landscape)
            }

            launch {
                setupFilterFromAssets(lifeStylePaths, FilterType.LifeStyle)
                setupFilterFromAssets(moodyPaths, FilterType.Moody)
                setupFilterFromAssets(naturalPaths, FilterType.Nature)
                setupFilterFromAssets(portraitPaths, FilterType.Portrait)
            }
        }
    }

    private fun setupFilterFromAssets(imagePaths: List<String>, filterType: FilterType) {
        Timber.i("starting setup photo Collage FromAssets...$imagePaths")
        val lutPathByFilterType: List<String> = lutPaths.findByFilterType(filterType)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val folder = when (filterType) {
                    FilterType.Nature -> NATURE_PATH
                    FilterType.Portrait -> PORTRAIT_PATH
                    FilterType.BAndW -> BAW_PATH
                    FilterType.Cinematic -> CINEMATIC_PATH
                    FilterType.Landscape -> LANDSCAPE_PATH
                    FilterType.LifeStyle -> LIFESTYLE_PATH
                    FilterType.Moody -> MOODY_PATH
                }
                imagePaths.forEachIndexed { index, imageName ->

                    val filter = Filter(
                        id = UUID.randomUUID().toString().hashCode(),
                        imagePath = "$ROOT_PATH$FILTER_PATH$folder/$imageName",
                        lutPath = lutPathByFilterType[index],
                        type = filterType
                    )
                    addFilter(filter, filterType)
                    Timber.i("$$ROOT_PATH$FILTER_PATH/$folder/$imageName")
                }
            } catch (e: Exception) {
                FirebaseEventUtils.recordException(e)
            }
        }
    }

    private fun addFilter(filter: Filter, type: FilterType) {
        Timber.i("Add filter type...: ${type.name}...")
        when (type) {
            FilterType.BAndW -> filterBAW.add(filter)
            FilterType.Cinematic -> filterCinematic.add(filter)
            FilterType.LifeStyle -> filterLifeStyle.add(filter)
            FilterType.Landscape -> filterLandscape.add(filter)
            FilterType.Moody -> filterMoody.add(filter)
            FilterType.Nature -> filterNature.add(filter)
            FilterType.Portrait -> filterPortrait.add(filter)
        }
    }

    private val _bitmapCache = MutableStateFlow<Bitmap?>(null)
    val bitmapCache: StateFlow<Bitmap?> = _bitmapCache.asStateFlow()

    fun setBitmapCache(bitmap: Bitmap?) {
        if (bitmap != null) {
            _bitmapCache.value = bitmap
        }
    }

    private val _uriCache = MutableStateFlow<Uri?>(null)
    val uriCache: StateFlow<Uri?> = _uriCache.asStateFlow()

    fun setUriCache(uri: Uri?) {
        _uriCache.value = uri
    }

    private val _bitmapToFilter = MutableStateFlow<Bitmap?>(null)
    val bitmapToFilter: StateFlow<Bitmap?> = _bitmapToFilter.asStateFlow()
    fun setBitmapToFilter(bitmap: Bitmap) {
        _bitmapToFilter.value = bitmap
    }

    // Lưu filter đang được chọn
    private val _selectedFilter = MutableStateFlow<Filter?>(null)
    val selectedFilter: StateFlow<Filter?> = _selectedFilter.asStateFlow()

    fun setSelectedFilter(filter: Filter?) {
        _selectedFilter.value = filter
    }

    fun getCurrentSelectedFilter(): Filter? {
        return selectedFilter.value
    }

    var inputUri: Uri? = null
    var outputUri: Uri? = null
    fun loadBitmap(
        imageModel: Any?,
        onLoadingStateChange: (Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                onLoadingStateChange(true)
                inputUri = imageModel as Uri?

                val cropDir = File(app.cacheDir, "crop")
                if (!cropDir.exists()) {
                    cropDir.mkdirs()
                }

                val outputFile = File(cropDir, "${System.currentTimeMillis()}out.png")
                outputUri = outputFile.toUri()

                Timber.d("Created output URI: $outputUri")
                onLoadingStateChange(false)
            } catch (e: Exception) {
                Timber.e(e, "Error loading bitmap")
                onLoadingStateChange(false)
            }
        }
    }

    ///////////////////////////////////////////////////////////////////
    //                  FUNCTION Save Image
    //////////////////////////////////////////////////////////////////
    var enableSaveImage: Boolean = false

    private val _resultImage = MutableStateFlow<Any?>(null)
    val resultBitmap: StateFlow<Any?> = _resultImage.asStateFlow()

    fun setResultBitmap(image: Any?) {
        viewModelScope.launch(Dispatchers.IO) {
            _resultImage.emit(image)
        }
    }

    fun enableRating() {
        sharePreference.save(IS_ENABLE_SHOW_RATE, true)
    }

    fun clearDataTemplate() {
        setBitmapCache(null)
        isTemplateFunction = false
        inputUri = null
        outputUri = null
    }
}

