package com.asoft.artsal.photo.ui.myphoto.viewmodel

import android.app.Application
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.asoft.artsal.photo.base.BaseViewModel
import com.asoft.artsal.photo.data.model.Photo
import com.asoft.artsal.photo.data.model.PhotoType
import com.asoft.artsal.photo.extensions.internal.getRecentPhotos
import com.asoft.artsal.photo.extensions.toDateString
import com.asoft.artsal.photo.utils.Const.APP_FOLDER
import com.asoft.artsal.photo.utils.Const.COLLAGE_FOLDER
import com.asoft.artsal.photo.utils.Const.ROOT
import com.asoft.artsal.photo.utils.Const.TEMPLATE_FOLDER
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import com.asoft.artsal.photo.utils.StorageUtils
import com.asoft.artsal.photo.utils.StorageUtils.deleteImageFromStorage
import com.asoft.artsal.photo.utils.StorageUtils.getImagesFromStorage
import com.minsap.ad.ads.MinSapBannerAd
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MyPhotoViewModel @Inject constructor(
    override val app: Application,
) : BaseViewModel(app) {

    val createBannerAd by lazy { MinSapBannerAd.create() }

    private var _recentPhotos = MutableStateFlow<List<Photo>>(emptyList())
    val recentPhotos: StateFlow<List<Photo>> = _recentPhotos.asStateFlow()

    private val _collagePhotos = MutableStateFlow<List<Photo>>(emptyList())
    val collagePhotos: StateFlow<List<Photo>> = _collagePhotos.asStateFlow()

    private val _templatePhotos = MutableStateFlow<List<Photo>>(emptyList())
    val templatePhotos: StateFlow<List<Photo>> = _templatePhotos.asStateFlow()

    private val _allPhotos = MutableStateFlow<List<Photo>>(emptyList())
    val allPhotos: StateFlow<List<Photo>> = _allPhotos.asStateFlow()

    private val _photoSelected = MutableStateFlow<Photo?>(null)
    val photoSelected: StateFlow<Photo?> = _photoSelected.asStateFlow()
    fun setPhotoSelected(photo: Photo) {
        _photoSelected.value = photo
    }

    private val _allPhotoSelected = MutableStateFlow<Pair<List<Photo>, PhotoType>>(Pair(emptyList(), PhotoType.Recent))
    val allPhotoSelected : StateFlow<Pair<List<Photo>, PhotoType>> = _allPhotoSelected.asStateFlow()
    fun setAllPhotoSelected(photo: List<Photo>, photoType: PhotoType) {
        _allPhotoSelected.value = Pair(photo, photoType)
    }

    private val _typePhotoSelected = MutableStateFlow<PhotoType?>(null)
    val typePhotoSelected: StateFlow<PhotoType?> = _typePhotoSelected.asStateFlow()
    fun setTypePhotoSelected(type: PhotoType) {
        _typePhotoSelected.value = type
    }

    private val _deleteResult = MutableStateFlow<StorageUtils.DeleteResult?>(null)
    val deleteResult: StateFlow<StorageUtils.DeleteResult?> = _deleteResult.asStateFlow()


    fun deletePhoto() {
        viewModelScope.launch(Dispatchers.IO) {
            val selectedPhoto = photoSelected.value
            if (selectedPhoto == null) {
                return@launch
            }

            val imagePath = selectedPhoto.path
            if (imagePath.isEmpty()) {
                return@launch
            }

            val file = File(imagePath)
            if (!file.exists()) {
                withContext(Dispatchers.Main) {
                    removePhotoFromLists(selectedPhoto)
                    _photoSelected.value = null
                }
                return@launch
            }

            val result = deleteImageFromStorage(app, imagePath)

            withContext(Dispatchers.Main) {
                when (result) {
                    is StorageUtils.DeleteResult.Success -> {
                        removePhotoFromLists(selectedPhoto)
                        _photoSelected.value = null
                        _deleteResult.value = result
                    }
                    is StorageUtils.DeleteResult.RequirePermission -> {
                        _deleteResult.value = result
                    }
                    is StorageUtils.DeleteResult.Error -> {
                        _deleteResult.value = result
                    }
                    is StorageUtils.DeleteResult.NotFound -> {
                        removePhotoFromLists(selectedPhoto)
                        _photoSelected.value = null
                        _deleteResult.value = result
                    }
                }
            }
        }
    }

    fun onDeletePermissionGranted() {
        val selectedPhoto = photoSelected.value ?: return
        removePhotoFromLists(selectedPhoto)
        _photoSelected.value = null
        _deleteResult.value = StorageUtils.DeleteResult.Success
    }

    fun clearDeleteResult() {
        _deleteResult.value = null
    }

    private fun removePhotoFromLists(deletedPhoto: Photo) {
        _allPhotos.value = _allPhotos.value.filter { it.id != deletedPhoto.id }
        _recentPhotos.value = _recentPhotos.value.filter { it.id != deletedPhoto.id }
        _collagePhotos.value = _collagePhotos.value.filter { it.id != deletedPhoto.id }
        _templatePhotos.value = templatePhotos.value.filter { it.id != deletedPhoto.id }
        val photoTypeCurrent = allPhotoSelected.value.second
        _allPhotoSelected.value = when (photoTypeCurrent) {
            PhotoType.Recent -> Pair (_recentPhotos.value, photoTypeCurrent)
            PhotoType.Template -> Pair (_templatePhotos.value, photoTypeCurrent)
            PhotoType.Collages -> Pair(_collagePhotos.value, photoTypeCurrent)
            PhotoType.AllPhoto -> Pair(_allPhotos.value, photoTypeCurrent)
            else -> Pair(_allPhotos.value, photoTypeCurrent)
        }
    }

    fun loadImages() {
        Timber.i("loading Images from storage...")
        viewModelScope.launch(Dispatchers.IO) {
            val collageFLow = async {
                val photoCollages = getImagesFromStorage(app, "$APP_FOLDER/$COLLAGE_FOLDER")
                _collagePhotos.value = setupDataPhotos(photoCollages, PhotoType.Collages)
            }
            val templateFlow = async {
                val photosTemplate = getImagesFromStorage(app, "$APP_FOLDER/$TEMPLATE_FOLDER")
                _templatePhotos.value = setupDataPhotos(photosTemplate, PhotoType.Template)

            }
            collageFLow.await()
            templateFlow.await()
            val allData = mutableListOf<Photo>()
            allData.addAll(collagePhotos.value)
            allData.addAll(templatePhotos.value)
            val dataAllPhotos = mutableListOf<Photo>()
            while (allData.isNotEmpty()) {
                val randomIndex = allData.indices.random()
                val photo = allData[randomIndex]
                dataAllPhotos.add(photo)
                allData.removeAt(randomIndex)
            }
            _allPhotos.value = dataAllPhotos
            _recentPhotos.value = dataAllPhotos.getRecentPhotos(10)
        }
    }

    private fun setupDataPhotos(
        photos: List<String>,
        photoType: PhotoType
    ): List<Photo> {
        Timber.i("setupDataPhotos $photoType...")
        return photos.map { path ->
            try {
                val timeStamp =
                    path.split("/").lastOrNull()?.split(".")?.first()?.toLongOrNull()
                        ?: System.currentTimeMillis()
                Photo(
                    id = UUID.randomUUID().hashCode(),
                    timeStamp = timeStamp,
                    timeString = timeStamp.toDateString("dd/MM/yyyy"),
                    path = path,
                    uri = (ROOT + path).toUri(),
                    type = photoType
                )
            } catch (e: Exception) {
                FirebaseEventUtils.recordException(e)
                Photo(0, 0, "", "", "".toUri(), photoType)
            }
        }
    }

    private val _dataUi = MutableStateFlow<DataUi>(DataUi())
    val dataUi: StateFlow<DataUi> = _dataUi.asStateFlow()

    fun setUiRecentData(isShowRecent: Boolean) {
        _dataUi.value = _dataUi.value.copy(isShowRecent = isShowRecent)
        updateUiEmptyViewData()
    }

    fun setUiTemplateData(isShowTemplate: Boolean) {
        _dataUi.value = _dataUi.value.copy(isShowTemplate = isShowTemplate)
        updateUiEmptyViewData()
    }


    fun setUiCollageData(isShowCollage: Boolean) {
        _dataUi.value = _dataUi.value.copy(isShowCollage = isShowCollage)
        updateUiEmptyViewData()
    }

    fun setUiAllPhotoData(isShowAllPhoto: Boolean) {
        _dataUi.value = _dataUi.value.copy(isShowAllPhoto = isShowAllPhoto)
        updateUiEmptyViewData()
    }

    fun updateUiEmptyViewData() {
        if (_recentPhotos.value.isEmpty() && _collagePhotos.value.isEmpty() && _allPhotos.value.isEmpty()) {
            _dataUi.value = DataUi(isShowEmptyView = true)
        } else {
            _dataUi.value = dataUi.value.copy(isShowEmptyView = false)
        }
    }
}

data class DataUi(
    val isShowRecent: Boolean = false,
    val isShowTemplate: Boolean = false,
    val isShowCollage: Boolean = false,
    val isShowAllPhoto: Boolean = false,
    val isShowEmptyView: Boolean = true,
)
