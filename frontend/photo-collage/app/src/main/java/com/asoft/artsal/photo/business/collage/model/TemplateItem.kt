package com.asoft.artsal.photo.business.collage.model


data class TemplateItem(
    val title: String,
    val photoItemList: List<PhotoItem> = emptyList()
)