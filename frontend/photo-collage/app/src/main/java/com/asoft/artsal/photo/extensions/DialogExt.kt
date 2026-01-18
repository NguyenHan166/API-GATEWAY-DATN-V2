package com.asoft.artsal.photo.extensions

import android.app.Activity
import android.app.Dialog
import android.content.res.Resources
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun DialogFragment.setWidthPercent(percentage: Int) {
    val percent = percentage.toFloat() / 100
    val dm = Resources.getSystem().displayMetrics
    val rect = dm.run { Rect(0, 0, widthPixels, heightPixels) }
    val percentHeight = rect.height() * percent
    dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, percentHeight.toInt())
}

@Suppress("DEPRECATION")
fun Dialog.hideNavigationBar() {
    try {
        window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // New way for Android 9.0 (API 28) and above
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    hide(WindowInsetsCompat.Type.navigationBars())
                    systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                // Old way for Android 7.0-8.1
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}

/**
 * Extension function to set up all necessary listeners to ensure navigation bar stays hidden
 */

@Suppress("DEPRECATION")
fun MaterialAlertDialogBuilder.showAndHideNav(): AlertDialog {
    val activity = context as? Activity
    val dialog = create()
    dialog.hideNavigationBar()
    // Show the dialog
    dialog.show()
    return dialog
}

fun Dialog.showLoadingAds(show: Boolean, fragment: Fragment) {
    if (show && !isShowing && !fragment.isStateSaved) {
        show()
    } else if (!show && isShowing) {
        dismiss()
    }
}
