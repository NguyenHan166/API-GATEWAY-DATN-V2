package com.asoft.artsal.photo.extensions

import android.os.Build

fun buildAtLeastApi33(): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
}

fun buildAtLeastApi31(): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
}