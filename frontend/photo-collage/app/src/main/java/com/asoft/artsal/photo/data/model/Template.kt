package com.asoft.artsal.photo.data.model

import androidx.annotation.DrawableRes
import com.artsal.photo.editor.collage.maker.R

data class Template(
    val id: Int,
    @DrawableRes val img: Int = 0,
    val radio: Float = 1 / 1f,
    val imagePath: String = "",
    val name: String = "",
    val typeView: TemplateViewType = TemplateViewType.Square,
    val layoutId: Int = 0,
    val layerFirstId: String? = null,
    val layerSecondId: String? = null
)

enum class TemplateViewType {
    Portrait, Square, Portrait2
}

enum class TemplateType(val title: Int) {
    Trending(R.string.trending), All(R.string.all), Birthdays(R.string.birthdays),
    SpecialDays(R.string.special_days), Holidays(R.string.holidays),
    Travel(R.string.travel), Seasons(R.string.seasons),
    Milestones(R.string.milestones), Moments(R.string.moments)
}

