package com.asoft.artsal.photo.data.model

import androidx.annotation.DrawableRes

data class Language(
    @DrawableRes val img: Int,
    val name: String,
    val code: String,
    val isChecked: Boolean = false
)
