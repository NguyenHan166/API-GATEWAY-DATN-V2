@file:Suppress("FunctionName")

package com.asoft.artsal.photo.business.collage.utils

import android.graphics.Path
import com.asoft.artsal.photo.business.collage.model.TemplateItem

/**
 * Created by Hoang Ngoc Linh on 7/25/2025.
 */
internal object FrameImageUtils {
    internal fun collage(
        frameName: String
    ): TemplateItem = TemplateItem(
        title = frameName
    )

    fun createHeartItem(top: Float, size: Float): Path {
        val path = Path()
        path.moveTo(top, top + size / 4)
        path.quadTo(top, top, top + size / 4, top)
        path.quadTo(top + size / 2, top, top + size / 2, top + size / 4)
        path.quadTo(top + size / 2, top, top + size * 3 / 4, top)
        path.quadTo(top + size, top, top + size, top + size / 4)
        path.quadTo(top + size, top + size / 2, top + size * 3 / 4, top + size * 3 / 4)
        path.lineTo(top + size / 2, top + size)
        path.lineTo(top + size / 4, top + size * 3 / 4)
        path.quadTo(top, top + size / 2, top, top + size / 4)
        return path
    }

    fun createTemplateItems(frameName: String): TemplateItem? {
        return when (frameName) {
            "collage_2_0.png" -> TwoFrameImage.collage_2_1()
            "collage_2_1.png" -> TwoFrameImage.collage_2_8()
            "collage_2_2.png" -> TwoFrameImage.collage_2_1()
            "collage_2_3.png" -> TwoFrameImage.collage_2_0()
            "collage_3_0.png" -> ThreeFrameImage.collage_3_12()
            "collage_3_1.png" -> ThreeFrameImage.collage_3_18()
            "collage_3_2.png" -> ThreeFrameImage.collage_3_0()
            "collage_3_3.png" -> ThreeFrameImage.collage_3_18()
            "collage_3_4.png" -> ThreeFrameImage.collage_3_17()
            "collage_3_5.png" -> ThreeFrameImage.collage_3_58()
            "collage_3_6.png" -> ThreeFrameImage.collage_3_18()
            "collage_4_0.png" -> FourFrameImage.collage_4_8()
            "collage_4_1.png" -> FourFrameImage.collage_4_0()
            "collage_4_2.png" -> FourFrameImage.collage_4_0()
            "collage_4_3.png" -> FourFrameImage.collage_4_1()
            "collage_4_4.png" -> FourFrameImage.collage_4_16()
            "collage_4_5.png" -> FourFrameImage.collage_4_0()
            "collage_5_0.png" -> FiveFrameImage.collage_5_0()
            "collage_5_1.png" -> FiveFrameImage.collage_5_4()
            "collage_5_2.png" -> FiveFrameImage.collage_5_20()
            "collage_5_3.png" -> FiveFrameImage.collage_5_17()
            "collage_5_4.png" -> FiveFrameImage.collage_5_3()
            "collage_5_5.png" -> FiveFrameImage.collage_5_5()
            else -> null
        }
    }
}
