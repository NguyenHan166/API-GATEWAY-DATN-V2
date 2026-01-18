package com.asoft.artsal.photo.utils

import android.content.Context
import com.asoft.artsal.photo.utils.Const.LUT_PATH
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetsPackManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun getAllModels(dataPath: String): List<String> {
        Timber.i("Loading filename list from assets: $dataPath...")
        return try {
            context.assets.list(dataPath)?.toList() ?: emptyList()
        } catch (e: IOException) {
            emptyList()
        }
    }

    /**
     * read all file .cube from assets/lut and copy into internal storage
     * @return List<String> - list path file copy
     */
    suspend fun copyLutFilesToInternalStorage(): List<String> = withContext(Dispatchers.IO) {
        val resultPaths = mutableListOf<String>()

        try {
            val cacheDir = context.cacheDir

            val assetManager = context.assets
            val lutFiles = assetManager.list(LUT_PATH) ?: emptyArray()

            lutFiles.forEach { fileName ->
                if (fileName.endsWith(".cube", ignoreCase = true)) {
                    val filePath = copyLutFileToCache(fileName, cacheDir)
                    filePath?.let { resultPaths.add(it) }
                }
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }

        resultPaths
    }

    /**
     * Copy a file LUT .cube from assets to internal cache
     */
    private suspend fun copyLutFileToCache(fileName: String, cacheDir: File): String? =
        withContext(Dispatchers.IO) {
            Timber.i("Copy LUT file $fileName to cache...")
            try {

                val targetFile = File(cacheDir, fileName)

                if (targetFile.exists()) {
                    Timber.w("file $fileName already exists in cache.")
                    return@withContext targetFile.absolutePath
                }

                context.assets.open("$LUT_PATH/$fileName").use { inputStream ->
                    FileOutputStream(targetFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                targetFile.absolutePath

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
}