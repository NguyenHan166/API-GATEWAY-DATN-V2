package com.asoft.artsal.photo.business.text.shape

import android.graphics.Paint
import com.asoft.artsal.photo.business.text.shape.AbstractShape

/**
 * Simple data class to be put in an ordered Stack
 */
open class ShapeAndPaint(
    val shape: AbstractShape,
    val paint: Paint
)