package com.asoft.artsal.photo.extensions

import android.os.Handler
import android.os.Looper


fun runOnUiThread(action: () -> Unit) {
    when {
        isMainThread() -> action.invoke()
        else -> Handler(Looper.getMainLooper()).post(Runnable(action))
    }
}

private fun isMainThread() = Looper.myLooper() == Looper.getMainLooper()

fun checkMainThread() {
    check(Looper.myLooper() == Looper.getMainLooper()) {
        "Expected to be called on the main thread but was " + Thread.currentThread().name
    }
}