package com.asoft.artsal.photo.base

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import com.asoft.artsal.photo.extensions.findActivity

class BasePopupWindow(
    private val context: Context,
    private val contentView: View
) : PopupWindow(contentView) {

    init {
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        isFocusable = true
        isOutsideTouchable = true

        // Set window flags
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setOnDismissListener {
            hideNavigationBar()
        }
        hideNavigationBar()
    }

    @Suppress("DEPRECATION")
    private fun hideNavigationBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ViewCompat.setOnApplyWindowInsetsListener(contentView) { view, windowInsets ->
                val controller = ViewCompat.getWindowInsetsController(view)
                controller?.apply {
                    hide(WindowInsetsCompat.Type.navigationBars())
                    systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
                // Handle bottom padding if needed
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.updatePadding(bottom = insets.bottom)
                WindowInsetsCompat.CONSUMED
            }
        } else {
            context.findActivity()?.let { act ->
                // Set layout fullscreen và ẩn navigation bar
                act.window?.setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                )

                act.window?.decorView?.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                or View.SYSTEM_UI_FLAG_LOW_PROFILE
                        )

                // Điều chỉnh layout params của window
                act.window?.attributes = act.window?.attributes?.apply {
                    flags = flags or WindowManager.LayoutParams.FLAG_FULLSCREEN
                }
            }
        }
    }

}