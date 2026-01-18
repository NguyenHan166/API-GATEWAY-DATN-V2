package com.asoft.artsal.photo.extensions

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Extension function to set status bar color and icon appearance
 * @param color The color for the status bar (default: transparent)
 * @param isLightStatusBar Whether to use dark icons on light background (true) or light icons on dark background (false)
 */
@Suppress("DEPRECATION")
fun Activity.setStatusBarStyle(
    color: Int = Color.TRANSPARENT,
    isLightStatusBar: Boolean = false
) {
    try {
        window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ (API 30+)
                window.statusBarColor = color

                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.isAppearanceLightStatusBars = isLightStatusBar
            } else {
                // Android 6.0+ (API 23+)
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = color

                if (isLightStatusBar) {
                    // Dark icons on light background
                    window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else {
                    // Light icons on dark background
                    window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and
                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
            }
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}

/**
 * Convenience function to set dark status bar (light background with dark icons)
 */
fun Activity.setLightStatusBar(color: Int = Color.WHITE) {
    setStatusBarStyle(color, isLightStatusBar = true)
}

/**
 * Convenience function to set light status bar (dark background with light icons)
 */
fun Activity.setDarkStatusBar(color: Int = Color.TRANSPARENT) {
    setStatusBarStyle(color, isLightStatusBar = false)
} 