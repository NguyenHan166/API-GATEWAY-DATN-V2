package com.asoft.artsal.photo.extensions.internal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.artsal.photo.editor.collage.maker.R
import com.asoft.artsal.photo.business.collage.model.MarginType
import com.asoft.artsal.photo.data.fontTypes
import com.asoft.artsal.photo.data.model.FilterType
import com.asoft.artsal.photo.data.model.FontType
import com.asoft.artsal.photo.data.model.Photo
import com.asoft.artsal.photo.data.model.TemplateType
import com.asoft.artsal.photo.data.model.TemplateViewType
import com.asoft.artsal.photo.data.model.TypeView
import com.asoft.artsal.photo.extensions.dpToPx
import com.asoft.artsal.photo.ui.collage.adapter.CollageType
import com.asoft.artsal.photo.ui.collage.viewmodel.Border
import timber.log.Timber

////////////////////////////////////////////////////
// COLLAGE
///////////////////////////////////////////////////
fun String.checkingTypeCollage(): CollageType {
    Timber.i("Checking collage type...")
    val numberPhoto = this.split("_")[1]
    return when (numberPhoto) {
        "2" -> CollageType.Collage2
        "3" -> CollageType.Collage3
        "4" -> CollageType.Collage4
        "5" -> CollageType.Collage5
        else -> CollageType.Collage2
    }
}

fun String.getTypeView(): TypeView {
    val matrix = this.split("_")
    val typePhoto = matrix[1]
    val typeCollage = matrix[2].split(".")[0]
    when (typePhoto) {
        "2" -> {
            return if (typeCollage == "2") TypeView.Portrait else TypeView.Square
        }

        "3" -> {
            return if (typeCollage == "2") TypeView.Landscape else if (typeCollage == "3") TypeView.Portrait else TypeView.Square
        }

        "4" -> {
            return if (typeCollage.toInt() <= 1) TypeView.Square else if (typeCollage == "2") TypeView.Landscape else TypeView.Portrait
        }

        "5" -> {
            return if (typeCollage.toInt() <= 1) TypeView.Landscape else if (typeCollage == "4") TypeView.Square else TypeView.Portrait
        }
    }
    return if (this.contains("landscape")) {
        TypeView.Landscape
    } else {
        TypeView.Portrait
    }
}


fun TypeView.getRadio(): Float {
    return when (this) {
        TypeView.Landscape -> 328 / 172f
        TypeView.Portrait -> 160 / 284.44f
        TypeView.Square -> 1f
    }
}

fun TypeView.getBorderByName(imageName: String): Border {
    return Border(0f, 0f, 0f)
}

fun String.getPhotoNumbers(): Int {
    return this.split("_")[1].toInt()
}

fun getSizeByType(typeView: TypeView): MarginType {
    return when (typeView) {
        TypeView.Landscape -> {
            MarginType(0, 156.dpToPx(), 0, 244.dpToPx())
        }

        TypeView.Portrait -> {
            MarginType(28.dpToPx(), 16.dpToPx(), 28.dpToPx(), 33.dpToPx())
        }

        TypeView.Square -> {
            MarginType(0, 96.dpToPx(), 0, 133.dpToPx())
        }
    }
}

fun Int.getFontStyleByFont(): FontType {
    val fontType = fontTypes.find { it -> it.font == this }
    return fontType ?: fontTypes[0]
}

fun List<String>.findByFilterType(filterType: FilterType): List<String> {
    when (filterType) {
        FilterType.BAndW -> {
            return this.filter { it.contains("baw") }
        }

        FilterType.Cinematic -> {
            return this.filter { it.contains("cinematic") }
        }

        FilterType.Landscape -> {
            return this.filter { it.contains("landscape") }
        }

        FilterType.LifeStyle -> {
            return this.filter { it.contains("lifestyle") }
        }

        FilterType.Moody -> {
            return this.filter { it.contains("moody") }
        }

        FilterType.Nature -> {
            return this.filter { it.contains("nature") }
        }

        FilterType.Portrait -> {
            return this.filter { it.contains("portrait") }
        }
    }
}

fun List<Photo>.getRecentPhotos(limit: Int = 10): List<Photo> {
    return this.sortedByDescending {
        it.timeStamp
    }.take(limit)
}

////////////////////////////////////////////////////
// TEMPLATE
///////////////////////////////////////////////////
fun String.getTemplateViewType(): TemplateViewType {
    val matrix = this.split("_")
    val typeTemplate = matrix[1]
    val typeImage = matrix[2].split(".")[0]
    when (typeTemplate) {
        "birthday" -> {
            return when (typeImage) {
                "05", "06" -> TemplateViewType.Portrait2
                "01", "02", "03", "04" -> TemplateViewType.Square
                else -> TemplateViewType.Portrait
            }
        }

        "holiday" -> {
            return TemplateViewType.Square
        }

        "special" -> {
            return TemplateViewType.Square
        }

        "moment" -> {
            return if (typeImage == "06") TemplateViewType.Portrait2 else TemplateViewType.Square
        }

        "travel" -> {
            return if (typeImage == "04") TemplateViewType.Portrait else TemplateViewType.Square
        }

        "season" -> {
            return TemplateViewType.Square
        }

        "milestone" -> {
            return if (typeImage == "01") TemplateViewType.Portrait2 else TemplateViewType.Square
        }
    }
    return TemplateViewType.Square
}

fun String.checkingTypeTemplate(): TemplateType {
    val typeStr = this.split("_")[1]
    return when (typeStr) {
        "birthday" -> TemplateType.Birthdays
        "holiday" -> TemplateType.Holidays
        "special" -> TemplateType.SpecialDays
        "moment" -> TemplateType.Moments
        "milestone" -> TemplateType.Milestones
        "season" -> TemplateType.Seasons
        else -> TemplateType.Travel
    }
}

fun TemplateViewType.getRadio(): Float {
    return when (this) {
        TemplateViewType.Portrait -> 160 / 284.44f
        TemplateViewType.Portrait2 -> 160 / 250f
        TemplateViewType.Square -> 1f
    }
}


fun String.getLayoutByFileName(): Int {
    return when (this) {
        "temp_birthday_01.png" -> R.layout.temp_birthday_01
        "temp_birthday_02.png" -> R.layout.temp_birthday_02
        "temp_birthday_03.png" -> R.layout.temp_birthday_01
        "temp_birthday_04.png" -> R.layout.temp_birthday_04
        "temp_birthday_05.png" -> R.layout.temp_birthday_05
        "temp_birthday_06.png" -> R.layout.temp_birthday_06
        "temp_holiday_01.png" -> R.layout.temp_holiday_01
        "temp_holiday_02.png" -> R.layout.temp_holiday_02
        "temp_holiday_03.png" -> R.layout.temp_holiday_03
        "temp_holiday_04.png" -> R.layout.temp_holiday_04
        "temp_holiday_05.png" -> R.layout.temp_holiday_05
        "temp_holiday_06.png" -> R.layout.temp_holiday_06
        "temp_holiday_07.png" -> R.layout.temp_holiday_07
        "temp_holiday_08.png" -> R.layout.temp_holiday_08
        "temp_holiday_09.png" -> R.layout.temp_holiday_09
        "temp_holiday_10.png" -> R.layout.temp_holiday_10
        "temp_milestone_01.png" -> R.layout.temp_milestone_01
        "temp_milestone_02.png" -> R.layout.temp_milestone_02
        "temp_milestone_03.png" -> R.layout.temp_milestone_03
        "temp_milestone_04.png" -> R.layout.temp_milestone_04
        "temp_milestone_05.png" -> R.layout.temp_milestone_05
        "temp_moment_01.png" -> R.layout.temp_moment_01
        "temp_moment_02.png" -> R.layout.temp_moment_02
        "temp_moment_03.png" -> R.layout.temp_moment_03
        "temp_moment_04.png" -> R.layout.temp_moment_04
        "temp_moment_05.png" -> R.layout.temp_moment_05
        "temp_moment_06.png" -> R.layout.temp_moment_06
        "temp_season_01.png" -> R.layout.temp_season_01
        "temp_season_02.png" -> R.layout.temp_season_02
        "temp_season_03.png" -> R.layout.temp_season_03
        "temp_season_04.png" -> R.layout.temp_season_04
        "temp_season_05.png" -> R.layout.temp_season_05
        "temp_travel_01.png" -> R.layout.temp_travel_01
        "temp_travel_02.png" -> R.layout.temp_travel_02
        "temp_travel_03.png" -> R.layout.temp_travel_03
        "temp_travel_04.png" -> R.layout.temp_travel_04
        "temp_travel_05.png" -> R.layout.temp_travel_05
        "temp_special_01.png" -> R.layout.temp_special_01
        "temp_special_02.png" -> R.layout.temp_special_02
        "temp_special_03.png" -> R.layout.temp_special_03
        "temp_special_04.png" -> R.layout.temp_special_04
        "temp_special_05.png" -> R.layout.temp_special_05
        "temp_special_06.png" -> R.layout.temp_special_06
        else -> {
            R.layout.temp_birthday_01
        }
    }
}

fun String.getLayerByFileName(firstLayer: List<String>): String? {
    val imageNameMaTrix = this.split("_")
    val typeImage = imageNameMaTrix[1]
    val positionImage = imageNameMaTrix[2]
    return findIdByTypeAndPosition(firstLayer, typeImage, positionImage)
}

fun findIdByTypeAndPosition(list: List<String>, typeImage: String, positionImage: String): String? {
    list.forEach { layerName ->
        val matrix = layerName.split("_")
        if (matrix[2] == typeImage && matrix[3] == positionImage) {
            return layerName
        }
    }
    return null
}

fun Uri.getBitmapFromUri(context: Context): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, this)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION") MediaStore.Images.Media.getBitmap(
                context.contentResolver,
                this
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

