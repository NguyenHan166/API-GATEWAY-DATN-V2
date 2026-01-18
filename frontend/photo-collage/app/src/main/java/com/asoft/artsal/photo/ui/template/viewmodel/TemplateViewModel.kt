package com.asoft.artsal.photo.ui.template.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.SparseArray
import androidx.core.content.FileProvider
import com.asoft.artsal.photo.base.BaseViewModel
import com.asoft.artsal.photo.data.model.Template
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import androidx.core.util.size
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

@HiltViewModel
class TemplateViewModel @Inject constructor(
    override val app: Application
) : BaseViewModel(app = app) {

    val imageBackup = SparseArray<Uri>()

    var positionSelected: Int = 0

    var imageCount: Int = 0

    fun updateImageBackup(uri: Uri) {
        imageBackup.put(positionSelected, uri)
        if (imageCount > 0 && imageCount == imageBackup.size) {
            _isSaveAvailable.value = true
        } else {
            _isSaveAvailable.value = false
        }
    }

    private val _isSaveAvailable = MutableStateFlow<Boolean>(false)
    val isSaveAvailableState: StateFlow<Boolean> = _isSaveAvailable.asStateFlow()
    fun updateIsSaveAvailable(isAvailable: Boolean) {
        _isSaveAvailable.update { isAvailable }
    }

    private val _templateSelected = MutableStateFlow<Template?>(null)
    val templateSelected = _templateSelected.asStateFlow()

    fun updateTemplate(template: Template) {
        _templateSelected.value = template
    }


    var enableSaveImage: Boolean = false

    fun clearDataTemplate() {
        imageBackup.clear()
        positionSelected = 0
        imageCount = 0
        _templateSelected.value = null
        _isSaveAvailable.value = false
        _isShowNav.value = false
    }

    private val _isShowNav = MutableStateFlow<Boolean>(false)
    val isShowNav = _isShowNav.asStateFlow()

    fun updateShowNav(isShow: Boolean) {
        _isShowNav.value = isShow
    }

     suspend fun persistBitmapToCacheWithProvider(bitmap: Bitmap): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val context = app.applicationContext
                val cacheDir = File(context.cacheDir, "template_filter")

                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }

                val fileName = "filtered_${System.currentTimeMillis()}.png"
                val file = File(cacheDir, fileName)

                BufferedOutputStream(FileOutputStream(file), 8192).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.flush()
                }

                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

            } catch (e: Exception) {
                FirebaseEventUtils.recordException(e)
                null
            }
        }
    }
}