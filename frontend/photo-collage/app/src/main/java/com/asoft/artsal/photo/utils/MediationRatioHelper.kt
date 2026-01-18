package com.asoft.artsal.photo.utils

import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.gms.ads.nativead.MediaView

object MediationRatioHelper {

    fun updateMediaViewWithRatio(mediaView: MediaView, aspectRatio: Float) {
        val params = mediaView.layoutParams
        if (params is ConstraintLayout.LayoutParams) {
            // Nếu sử dụng ConstraintLayout
            // Lưu ý: Trong ConstraintLayout, aspect ratio được định nghĩa là width:height
            // Nên cần chuyển từ width/height sang width:height
            params.dimensionRatio = "H,${aspectRatio}"
        } else {
            // Nếu sử dụng các layout khác
            val width = mediaView.width
            if (width > 0) {
                params.height = (width / aspectRatio).toInt()
            }
        }
        mediaView.layoutParams = params
        mediaView.requestLayout()
    }

    // Thiết lập tỉ lệ mặc định dựa trên network
    fun setDefaultRatioByNetwork(mediaView: MediaView, adSourceName: String?) {
        val ratio = when {
            adSourceName?.contains("facebook", ignoreCase = true) == true ||
                    adSourceName?.contains("meta", ignoreCase = true) == true -> 1.91f

            adSourceName?.contains("unity", ignoreCase = true) == true ||
                    adSourceName?.contains("applovin", ignoreCase = true) == true ||
                    adSourceName?.contains("ironsource", ignoreCase = true) == true -> 1.78f // 16:9

            else -> 1.91f // AdMob và các network khác
        }

        val params = mediaView.layoutParams
        if (params is ConstraintLayout.LayoutParams) {
            params.dimensionRatio = "H,$ratio:1"
        }
        mediaView.layoutParams = params
        mediaView.requestLayout()
    }
}