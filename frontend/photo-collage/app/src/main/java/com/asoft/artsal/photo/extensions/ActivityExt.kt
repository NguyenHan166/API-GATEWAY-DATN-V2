package com.asoft.artsal.photo.extensions

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.artsal.photo.editor.collage.maker.BuildConfig
import java.io.File
import java.io.FileOutputStream

@Suppress("DEPRECATION")
fun Activity.shareLink(link: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val rootView = window.decorView
        val controller = WindowInsetsControllerCompat(window, rootView)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        controller.show(WindowInsetsCompat.Type.statusBars())

        ShareCompat.IntentBuilder(this)
            .setText(link)
            .setType("text/plain")
            .startChooser()
    } else {
        // For Android 7.0 and below
        ShareCompat.IntentBuilder(this)
            .setText(link)
            .setType("text/plain")
            .startChooser()

        Handler(mainLooper).postDelayed({
            window?.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LOW_PROFILE)
        }, 300L)
    }
}


fun shareLink(link: String, context: Context) {
    ShareCompat.IntentBuilder(context)
        .setText(link)
        .setType("text/plain")
        .startChooser()
}

fun Activity.openChPlay(fullLink: String? = null) {
    try {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/details?id=$packageName".toUri()
            )
        )
    } catch (ex: ActivityNotFoundException) {
        startActivity(Intent(Intent.ACTION_VIEW, fullLink?.toUri()))
    }
}

@Suppress("DEPRECATION")
fun Activity.hideNavigationBar() {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.navigationBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window?.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            window?.decorView?.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LOW_PROFILE
                    )
            window?.attributes = window?.attributes?.apply {
                flags = flags or WindowManager.LayoutParams.FLAG_FULLSCREEN
            }
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}

fun Activity.sendFeedback(email: String, subject: String, rate: Int? = null) {
    try {
        val appName = applicationInfo.loadLabel(packageManager).toString()
        val appVersion = BuildConfig.VERSION_NAME
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        val osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        val emailBody = """
            App Name: $appName
            Version: $appVersion
            Rate: $rate
            Device Model Name: $deviceModel
            OS: $osVersion
            Feedback Data:
        """.trimIndent()

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, emailBody)
        }

        if (emailIntent.resolveActivity(packageManager) != null) {
            startActivity(emailIntent)
        } else {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}

fun Activity.hideKeyboard() {
    val imm =
        this.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    val currentFocus = this.currentFocus
    if (currentFocus != null) {
        imm?.hideSoftInputFromWindow(currentFocus.windowToken, 0)
    }
}



