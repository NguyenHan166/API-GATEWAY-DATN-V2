package com.asoft.artsal.photo.data.model

import androidx.annotation.DrawableRes
import com.asoft.artsal.photo.ui.collage.viewmodel.Border
import com.asoft.artsal.photo.ui.collage.adapter.CollageType

data class Collage(
    val id: Int,
    @DrawableRes val img: Int = 0,
    val imagePath: String = "",
    val type: TypeEditor,
    val collageName: String = "",
    val collageType: CollageType,
    val photoNumbers: Int,
    val radio: Float = 1 / 1f,
    val typeView: TypeView = TypeView.Square,
    val border: Border = Border(0f,0f,0f),
)

enum class TypeEditor {
    Collage, Template
}

enum class TypeView {
    Landscape, Portrait, Square
}