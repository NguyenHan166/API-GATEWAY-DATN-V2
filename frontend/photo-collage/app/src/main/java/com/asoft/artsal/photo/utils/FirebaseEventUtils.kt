package com.asoft.artsal.photo.utils

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.crashlytics.crashlytics
import java.lang.Exception

object FirebaseEventUtils {

    fun logEventTracking(context: Context?, eventName: String) {
        context?.let {
            FirebaseAnalytics.getInstance(it).logEvent(eventName, bundleOf())
        }
    }

    fun logEventTrackingWithParams(context: Context?, eventName: String, bundle: Bundle) {
        context?.let {
            FirebaseAnalytics.getInstance(it).logEvent(eventName, bundle)
        }
    }

    fun logEventScreen(screenName: String, screenClass: String) {
        val firebaseAnalytics = Firebase.analytics
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
    }

    fun recordException(exception: Exception) {
        val crashlytics = Firebase.crashlytics
        crashlytics.recordException(exception)
    }

}