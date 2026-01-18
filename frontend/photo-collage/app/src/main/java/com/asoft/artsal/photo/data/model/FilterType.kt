package com.asoft.artsal.photo.data.model

data class Filter(
    val id: Int,
    val imagePath: String = "",
    val lutPath: String = "",
    val type: FilterType = FilterType.Moody,
)

data class FilterTypeItem(
    val res: Int,
    val type: FilterType,
)

enum class FilterType {
    Moody, Nature, Portrait, BAndW, Cinematic, Landscape, LifeStyle
}
