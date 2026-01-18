package com.asoft.artsal.photo.extensions

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Rect
import android.net.*
import android.view.Gravity
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.asoft.artsal.photo.base.network.NetworkStatusChecker
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.*
import androidx.core.net.toUri

fun Context.readAssetAsText(fileName: String): String = assets.open(fileName)
    .bufferedReader()
    .use { it.readText() }

fun Context.toast(resId: Int) = runOnUiThread {
    Toast.makeText(this, resId, Toast.LENGTH_SHORT).apply {
        show()
    }
}

fun Context.toast(text: CharSequence) = runOnUiThread {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).apply { show() }
}

fun Context.toastGravity(text: CharSequence) = runOnUiThread {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).apply {
        setGravity(Gravity.CENTER, 0, 0)
        show()
    }
}

fun Context.toastGravityCustom(text: CharSequence, gravity: Int) = runOnUiThread {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).apply {
        setGravity(gravity, 10, 10)
        show()
    }
}

fun Context.displayToastAboveView(v: View, messageId: Int) {
    var xOffset = 0
    var yOffset = 0
    val gvr = Rect()
    val parent = v.parent as View
    val parentHeight = parent.height
    if (v.getGlobalVisibleRect(gvr)) {
        val root = v.rootView
        val halfWidth = root.right / 2
        val halfHeight = root.bottom / 2
        val parentCenterX: Int = (gvr.right - gvr.left) / 2 + gvr.left
        val parentCenterY: Int = (gvr.bottom - gvr.top) / 2 + gvr.top
        yOffset = if (parentCenterY <= halfHeight) {
            -(halfHeight - parentCenterY) - parentHeight
        } else {
            parentCenterY - halfHeight - parentHeight
        }
        if (parentCenterX < halfWidth) {
            xOffset = -(halfWidth - parentCenterX)
        }
        if (parentCenterX >= halfWidth) {
            xOffset = parentCenterX - halfWidth
        }
    }
    val toast = Toast.makeText(this, messageId, Toast.LENGTH_SHORT)
    toast.setGravity(Gravity.CENTER, xOffset, yOffset)
    toast.show()
}

fun Context.showSnackBar(
    view: View,
    message: String,
    duration: Int = Snackbar.LENGTH_SHORT
) {
    Snackbar.make(view, message, duration).show()
}

fun Context.networkAvailable(): Flow<Boolean> = callbackFlow {
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            trySend(true).isSuccess
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            trySend(false)
        }
    }

    val networkManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    networkManager.registerNetworkCallback(
        NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build(),
        callback
    )
    awaitClose {
        networkManager.unregisterNetworkCallback(callback)
    }
}

@Suppress("unused")
inline val Any?.unit
    get() = Unit

fun Context.popToRoot(activity: Activity) {
    val intent = Intent(this, activity::class.java)
    intent.putExtra("PopToRoot", "")
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    startActivity(intent)
}

fun Context.setAppLocale(language: String): Context? {
    try {
        val locale = if (language.contains("-r")) {
            val parts = language.split("-r")
            val lang = parts[0]
            val country = parts[1]
            Locale(lang, country)
        } else {
            Locale(language)
        }
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        return createConfigurationContext(config)
    } catch (ex: Exception) {
        return null
    }
}

val Context.hasNetwork: Boolean
    get() = NetworkStatusChecker(
        this.getSystemService(
            ConnectivityManager::class.java
        )
    ).hasInternetConnection()

fun Context.copyToClipboard(text: CharSequence) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("label", text)
    clipboard.setPrimaryClip(clip)
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

fun Context.openBrowser(
    url: String
) {
    try {
        val browserIntent = Intent(Intent.ACTION_VIEW, url.toUri())
        this.startActivity(browserIntent)
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}

fun Context.openWithGoogleSearch(key: String) {
    val url = "https://www.google.com/search?q=$key"
    openBrowser(url)
}


