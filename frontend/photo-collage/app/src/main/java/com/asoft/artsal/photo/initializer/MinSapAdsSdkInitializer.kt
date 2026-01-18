package com.asoft.artsal.photo.initializer

import android.content.Context
import androidx.startup.Initializer
import com.artsal.photo.editor.collage.maker.BuildConfig
import com.artsal.photo.editor.collage.maker.R
import com.minsap.ad.core.AdConfig
import com.minsap.ad.core.MinSapAd

class MinSapAdsSdkInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        val isTestMode = BuildConfig.DEBUG
        val config = AdConfig.Builder()
            .setTestMode(isTestMode)
            .setLoggingEnabled(isTestMode)
            .setAppsFlyerKey(context.getString(R.string.appflayer_key))
            .setConsentFlowEnabled(false)
            .build()

        MinSapAd.initializeConfig(context = context, config = config)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

}