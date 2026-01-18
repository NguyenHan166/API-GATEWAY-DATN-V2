package com.asoft.artsal.photo.data

import com.artsal.photo.editor.collage.maker.R
import com.asoft.artsal.photo.data.model.FilterType
import com.asoft.artsal.photo.data.model.FilterTypeItem
import com.asoft.artsal.photo.data.model.FontType
import com.asoft.artsal.photo.ui.ai.adapter.StyleItem
import java.util.UUID

val fontTypes: List<FontType> = listOf(
    FontType(
        id = UUID.randomUUID().hashCode(),
        fontName = "Public Sans",
        showName = "Casual",
        font = R.font.public_sans_medium
    ),
    FontType(
        id = UUID.randomUUID().hashCode(),
        fontName = "League Spartan",
        showName = "Classic",
        font = R.font.league_spartan_semibold
    ),
    FontType(
        id = UUID.randomUUID().hashCode(),
        fontName = "Oxanium",
        showName = "Modern",
        font = R.font.oxanium_medium
    ),
    FontType(
        id = UUID.randomUUID().hashCode(),
        fontName = "Bagel Fat One",
        showName = "Blobby",
        font = R.font.bagel_fat_one_regular
    ),
    FontType(
        id = UUID.randomUUID().hashCode(),
        fontName = "Parisienne",
        showName = "Elegant",
        font = R.font.parisienne_regular
    ),
    FontType(
        id = UUID.randomUUID().hashCode(),
        fontName = "Italianno",
        showName = "Fancy",
        font = R.font.italianno_regular
    ),
    FontType(
        id = UUID.randomUUID().hashCode(),
        fontName = "Yatra One",
        showName = "Marker",
        font = R.font.yatra_one_regular
    ),
    FontType(
        id = UUID.randomUUID().hashCode(),
        fontName = "Pacifico",
        showName = "Cute",
        font = R.font.pacifico_regular
    ),
    FontType(
        id = UUID.randomUUID().hashCode(),
        fontName = "Caveat",
        showName = "Handwrite",
        font = R.font.caveat_regular
    ),
    FontType(
        id = UUID.randomUUID().hashCode(),
        fontName = "Crimson Text",
        showName = "Old Style",
        font = R.font.crimson_text_semibold
    ),
    FontType(
        id = UUID.randomUUID().hashCode(),
        fontName = "Bebas Neue",
        showName = "Strong",
        font = R.font.bebas_neue_regular
    ),
    FontType(
        id = UUID.randomUUID().hashCode(),
        fontName = "Exo 2",
        showName = "Editor",
        font = R.font.exo2_semibold
    ),
    FontType(
        id = UUID.randomUUID().hashCode(),
        fontName = "Limelight",
        showName = "Deco",
        font = R.font.limelight_regular
    ),
)

val styleComicSingle = listOf<StyleItem>(
    StyleItem(
        id = "anime",
        name = "Anime",
        imageRes = R.drawable.img_anime,
        styleValue = "Anime"
    ),
    StyleItem(
        id = "manga",
        name = "Manga",
        imageRes = R.drawable.ic_manga,
        styleValue = "Manga"
    ),
    StyleItem(
        id = "webtoon",
        name = "Webtoon",
        imageRes = R.drawable.ic_webtoon,
        styleValue = "Webtoon"
    )
)

val styleComicMultiple = listOf<StyleItem>(
    StyleItem(
        id = "anime",
        name = "Anime",
        imageRes = R.drawable.img_anime,
        styleValue = "Anime"
    ),
    StyleItem(
        id = "manga",
        name = "Manga",
        imageRes = R.drawable.ic_manga,
        styleValue = "Manga"
    ),
    StyleItem(
        id = "Photographic",
        name = "Photographic",
        imageRes = R.drawable.ic_photographic,
        styleValue = "Photographic"
    ),
    StyleItem(
        id = "Digital Art",
        name = "Digital Art",
        imageRes = R.drawable.ic_digital,
        styleValue = "Digital Art"
    ),
    StyleItem(
        id = "Pixel art",
        name = "Pixel art",
        imageRes = R.drawable.ic_pixel_art,
        styleValue = "Pixel art"
    ),
    StyleItem(
        id = "Fantasy art",
        name = "Fantasy art",
        imageRes = R.drawable.ic_fantasy_art,
        styleValue = "Fantasy art"
    ),
    StyleItem(
        id = "Neonpunk",
        name = "Neonpunk",
        imageRes = R.drawable.ic_neonpunk,
        styleValue = "Neonpunk"
    ),
    StyleItem(
        id = "3D Model",
        name = "3D Model",
        imageRes = R.drawable.ic_3d_model,
        styleValue = "3D Model"
    )
)

val filterTypes: List<FilterTypeItem> = listOf(
    FilterTypeItem(res = R.drawable.img_moody, type = FilterType.Moody),
    FilterTypeItem(res = R.drawable.img_nature, type = FilterType.Nature),
    FilterTypeItem(res = R.drawable.img_portrait, type = FilterType.Portrait),
    FilterTypeItem(res = R.drawable.img_bandw, type = FilterType.BAndW),
    FilterTypeItem(res = R.drawable.img_cinematic, type = FilterType.Cinematic),
    FilterTypeItem(res = R.drawable.img_landscape, type = FilterType.Landscape),
    FilterTypeItem(res = R.drawable.img_lifestyle, type = FilterType.LifeStyle),
)

