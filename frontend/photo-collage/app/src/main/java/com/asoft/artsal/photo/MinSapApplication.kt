package com.asoft.artsal.photo

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import com.minsap.ad.consent.ConsentManager
import com.minsap.ad.core.AdConfig
import com.minsap.ad.core.MinSapAd
import com.asoft.artsal.photo.utils.SharePreference
import com.artsal.photo.editor.collage.maker.BuildConfig
import com.artsal.photo.editor.collage.maker.R
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MinSapApplication : MultiDexApplication() {
    @Inject
    lateinit var processLifecycleObserver: ProcessLifecycleObserver
    @Inject
    lateinit var processActivityLifecycleObserver: ProcessActivityLifecycleObserver
    @Inject
    lateinit var sharePreference: SharePreference

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        registerActivityLifecycleCallbacks(processActivityLifecycleObserver)
        ProcessLifecycleOwner.get().lifecycle.addObserver(processLifecycleObserver)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
//        initAdConfig()
    }

    private fun initLocale() {
        val savedLocaleCode = sharePreference.get<String>(SharePreference.LANGUAGE_APP, "en")
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(savedLocaleCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    override fun onTerminate() {
        ConsentManager.getInstance().cleanup()
        MinSapAd.cleanupResources()
        super.onTerminate()
    }
}