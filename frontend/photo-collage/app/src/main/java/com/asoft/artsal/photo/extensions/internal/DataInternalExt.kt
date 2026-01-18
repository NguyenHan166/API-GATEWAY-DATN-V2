package com.asoft.artsal.photo.extensions.internal

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

fun FirebaseRemoteConfig.getAdsKey(context: Context?, key: String): Boolean {
    if (context == null) return true
    return this.getBoolean(key)
}
