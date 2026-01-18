package com.asoft.artsal.photo.ui.gallery

import android.app.Application
import android.app.PendingIntent
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.viewModelScope
import com.artsal.photo.editor.collage.maker.R
import com.asoft.artsal.photo.base.BaseViewModel
import com.asoft.artsal.photo.data.model.GalleryPhoto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GalleryPhotoViewModel @Inject constructor(
    override val app: Application,
) : BaseViewModel(app) {

    var photoLimited: Int = 1
        private set

    var galleryType: String? = null

    fun setPhotoLimit(limit: Int) {
        photoLimited = limit
    }

    private val _listImages = MutableStateFlow<List<GalleryPhoto>>(emptyList())
    val listImages = _listImages.asStateFlow()

    // Selection State
    private var _imagesSelected = MutableStateFlow<List<GalleryPhoto>?>(null)
    var imagesSelected = _imagesSelected.asStateFlow()

    private val _permissionRequest = MutableSharedFlow<Pair<Uri, PendingIntent>>()
    val permissionRequest: SharedFlow<Pair<Uri, PendingIntent>> = _permissionRequest.asSharedFlow()

    // Toast message for limit exceeded
    private val _showToastMessage = MutableSharedFlow<String>()
    val showToastMessage: SharedFlow<String> = _showToastMessage.asSharedFlow()

    // ===== LOAD DATA FUNCTIONS =====
    fun loadImages() {
        viewModelScope.launch(Dispatchers.IO) {
            val images = loadMediaFromStore(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATA
                ),
                MEDIA_TYPE_IMAGE
            )

            withContext(Dispatchers.Main) {
                _listImages.value = images
            }
        }
    }

    private suspend fun loadMediaFromStore(
        contentUri: Uri,
        projection: Array<String>,
        mediaType: String
    ): List<GalleryPhoto> {
        val mediaList = mutableListOf<GalleryPhoto>()
        val sortOrder = "${MediaStore.MediaColumns.DATE_TAKEN} DESC"

        try {
            val query = app.contentResolver.query(
                contentUri,
                projection,
                null,
                null,
                sortOrder
            )

            query?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(projection[0])
                val displayNameColumn = cursor.getColumnIndexOrThrow(projection[1])
                val dataColumn = cursor.getColumnIndexOrThrow(projection[2])

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val displayName = cursor.getString(displayNameColumn) ?: "unknown"
                    val filePath = cursor.getString(dataColumn) ?: continue
                    val itemContentUri = ContentUris.withAppendedId(contentUri, id)

                    mediaList.add(
                        GalleryPhoto(
                            uri = itemContentUri
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading $mediaType")
        }

        return mediaList
    }

    // ===== SELECTION FUNCTIONS =====
    fun selectImage(galleryPhoto: GalleryPhoto) {
        viewModelScope.launch {
            val currentList = _listImages.value.toMutableList()
            val currentPhoto = currentList.find { it.uri == galleryPhoto.uri }
            val currentSelectedCount = currentList.count { it.isSelected }

            if (galleryPhoto.isSelected && currentPhoto?.isSelected != true) {
                if (currentSelectedCount >= photoLimited) {
                    _showToastMessage.emit(app.getString(R.string.you_have_selected_enough_photos))
                    return@launch
                }
            }

            val updatedList = currentList.map { photo ->
                if (photo.uri == galleryPhoto.uri) {
                    photo.copy(isSelected = galleryPhoto.isSelected)
                } else {
                    photo
                }
            }

            _listImages.value = updatedList
            updateSelectionState(updatedList)
        }
    }

    private fun updateSelectionState(photoList: List<GalleryPhoto>) {
        val selectedPhotos = photoList.filter { it.isSelected }
        _imagesSelected.value = selectedPhotos.takeIf { it.isNotEmpty() }
    }

    fun clearData() {
        _listImages.value = emptyList<GalleryPhoto>()
        _imagesSelected.value = null
    }

    companion object {
        const val MEDIA_TYPE_IMAGE = "image"
    }
}