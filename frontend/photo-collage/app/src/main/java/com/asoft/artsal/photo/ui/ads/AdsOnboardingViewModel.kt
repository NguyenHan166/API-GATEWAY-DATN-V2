package com.asoft.artsal.photo.ui.ads

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.asoft.artsal.photo.base.BaseViewModel
import com.asoft.artsal.photo.utils.AdsIdUtils
import com.asoft.artsal.photo.utils.RemoteConfigAdsConst
import com.asoft.artsal.photo.utils.SharePreference
import com.asoft.artsal.photo.utils.SingleLiveEvent
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.minsap.ad.ads.MinSapInterstitialAd
import com.minsap.ad.ads.MinSapNativeAd
import com.minsap.ad.ads.common.InterstitialAdState
import com.minsap.ad.ads.common.NativeAdState
import com.minsap.ad.config.AdError
import com.minsap.ad.config.InterstitialAdValue
import com.minsap.ad.config.NativeAdValue
import com.minsap.ad.listener.InterstitialAdCallBack
import com.minsap.ad.listener.NativeAdCallBack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdsOnboardingViewModel @Inject constructor(
    override val app: Application,
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
    private val sharePreference: SharePreference,
) : BaseViewModel(app = app) {
    private var isFirstOpen: Boolean = true

    init {
        isFirstOpen = sharePreference.get<Boolean>(SharePreference.IS_FIRST_OPEN_APP, true) ?: true
    }

    private val _adNativeBannerSplash = MutableStateFlow<NativeAdState?>(null)
    val adNativeBannerSplash: StateFlow<NativeAdState?> = _adNativeBannerSplash.asStateFlow()
    private val _createAdNativeBannerSplash by lazy { MinSapNativeAd.create() }
    val createAdNativeBannerSplash: MinSapNativeAd = _createAdNativeBannerSplash

    fun getAdNativeBannerSplash() = _adNativeBannerSplash

    fun preloadAdNativeBannerSplash() {
        if (firebaseRemoteConfig.getBoolean(RemoteConfigAdsConst.BANNER_SPLASH_SHOW)) {
            if (_adNativeBannerSplash.value != null) return
            viewModelScope.launch(Dispatchers.IO) {
                _createAdNativeBannerSplash.loadNativeAdResult(
                    context = app,
                    adUnitId = AdsIdUtils.BANNER_SPLASH,
                    nameAd = "Native Banner Ads",
                    callbackResult = object : NativeAdCallBack() {
                        override fun onAdLoaded(nativeAdValue: NativeAdValue) {
                            _adNativeBannerSplash.value =
                                NativeAdState.NativeAdData(nativeAdValue = nativeAdValue)
                        }

                        override fun onAdFailedToLoad(error: AdError) {
                            _adNativeBannerSplash.value =
                                NativeAdState.NativeAdError(adError = error)
                        }

                        override fun onAdClicked() {
                            _adNativeBannerSplash.value = null
                            preloadAdNativeBannerSplash()
                        }
                    }
                )
            }
        }
    }

    private val _adInterHighSplash = SingleLiveEvent<InterstitialAdState?>()
    val adInterHighSplash: LiveData<InterstitialAdState?> = _adInterHighSplash
    private val _createInterHighSplash by lazy { MinSapInterstitialAd.create() }
    val createInterHighSplash: MinSapInterstitialAd = _createInterHighSplash

    fun preloadInterHighAdSplash() {
        val adsId =
            if (isFirstOpen) AdsIdUtils.INTER_SPLASH_HIGH else AdsIdUtils.INTER_SPLASH_HIGH_2ND
        val adsConfig = if (isFirstOpen) RemoteConfigAdsConst.INTER_SPLASH_SHOW else
            RemoteConfigAdsConst.INTER_SPLASH_2ND_SHOW
        if (firebaseRemoteConfig.getBoolean(adsConfig)) {
            if (_adInterHighSplash.value != null) return
            viewModelScope.launch {
                delay(2000L)
                _createInterHighSplash.loadInterstitialAdResult(
                    context = app,
                    adUnitId = adsId,
                    nameAd = "High Splash",
                    callBackResult = object : InterstitialAdCallBack() {
                        override fun onAdLoaded(ad: InterstitialAdValue) {
                            super.onAdLoaded(ad)
                            _adInterHighSplash.call(
                                InterstitialAdState.InterstitialAdData(
                                    interstitialAdValue = ad
                                )
                            )
                        }

                        override fun onAdFailedToLoad(error: AdError) {
                            super.onAdFailedToLoad(error)
                            _adInterHighSplash.call(
                                InterstitialAdState.InterstitialAdError(
                                    adError = error
                                )
                            )
                        }
                    }
                )
            }
        }
    }

    private val _adInterSplash = SingleLiveEvent<InterstitialAdState?>()
    val adInterSplash: LiveData<InterstitialAdState?> = _adInterSplash
    private val _createInterSplash by lazy { MinSapInterstitialAd.create() }
    val createInterSplash: MinSapInterstitialAd = _createInterSplash

    fun preloadInterAdSplash() {
        val adsId = if (isFirstOpen) AdsIdUtils.INTER_SPLASH else AdsIdUtils.INTER_SPLASH_2ND
        if (_adInterSplash.value != null) return
        _createInterSplash.loadInterstitialAdResult(
            context = app,
            adUnitId = adsId,
            nameAd = "Splash",
            callBackResult = object : InterstitialAdCallBack() {
                override fun onAdLoaded(ad: InterstitialAdValue) {
                    super.onAdLoaded(ad)
                    _adInterSplash.call(
                        InterstitialAdState.InterstitialAdData(
                            interstitialAdValue = ad
                        )
                    )
                }

                override fun onAdFailedToLoad(error: AdError) {
                    super.onAdFailedToLoad(error)
                    _adInterSplash.call(
                        InterstitialAdState.InterstitialAdError(
                            adError = error
                        )
                    )
                }
            }
        )
    }


    private val _adNativeLanguage1 = MutableStateFlow<NativeAdState?>(null)
    val adNativeLanguage1: StateFlow<NativeAdState?> = _adNativeLanguage1.asStateFlow()
    private val _createAdNativeLanguage1 by lazy { MinSapNativeAd.create() }
    val createAdNativeLanguage1: MinSapNativeAd = _createAdNativeLanguage1

    fun getAdNativeLanguage1() = _adNativeLanguage1

    fun preloadNativeAdLanguage1() {
        if (firebaseRemoteConfig.getBoolean(RemoteConfigAdsConst.NATIVE_LANGUAGE_1_SHOW)) {
            if (_adNativeLanguage1.value != null) return
            viewModelScope.launch(Dispatchers.IO) {
                _createAdNativeLanguage1.loadNativeAdPriorityAlternate(
                    context = app,
                    adUnitIdPriority = AdsIdUtils.NATIVE_LANGUAGE1_HIGH,
                    adUnitIdNormal = AdsIdUtils.NATIVE_LANGUAGE1,
                    nameAdPriority = "High Language 1",
                    nameAdNormal = "Language 1",
                    callbackResult = object : NativeAdCallBack() {
                        override fun onAdLoaded(nativeAdValue: NativeAdValue) {
                            _adNativeLanguage1.value =
                                NativeAdState.NativeAdData(nativeAdValue = nativeAdValue)
                        }

                        override fun onAdFailedToLoad(error: AdError) {
                            _adNativeLanguage1.value = NativeAdState.NativeAdError(adError = error)
                        }

                        override fun onAdClicked() {
                            _adNativeLanguage1.value = null
                            preloadNativeAdLanguage1()
                        }
                    }
                )
            }
        }
    }

    private val _adNativeLanguage2 = MutableStateFlow<NativeAdState?>(null)
    val adNativeLanguage2: StateFlow<NativeAdState?> = _adNativeLanguage2.asStateFlow()
    private val _createAdNativeLanguage2 by lazy { MinSapNativeAd.create() }
    val createAdNativeLanguage2: MinSapNativeAd = _createAdNativeLanguage2

    fun getAdNativeLanguage2() = _adNativeLanguage2

    fun preloadNativeAdLanguage2() {
        if (firebaseRemoteConfig.getBoolean(RemoteConfigAdsConst.NATIVE_LANGUAGE_2_SHOW)) {
            if (_adNativeLanguage2.value != null) return
            viewModelScope.launch(Dispatchers.IO) {
                _createAdNativeLanguage2.loadNativeAdPriorityAlternate(
                    context = app,
                    adUnitIdPriority = AdsIdUtils.NATIVE_LANGUAGE2_HIGH,
                    adUnitIdNormal = AdsIdUtils.NATIVE_LANGUAGE2,
                    nameAdPriority = "High Language 2",
                    nameAdNormal = "Language 2",
                    callbackResult = object : NativeAdCallBack() {
                        override fun onAdLoaded(nativeAdValue: NativeAdValue) {
                            _adNativeLanguage2.value =
                                NativeAdState.NativeAdData(nativeAdValue = nativeAdValue)
                        }

                        override fun onAdFailedToLoad(error: AdError) {
                            _adNativeLanguage2.value = NativeAdState.NativeAdError(adError = error)
                        }

                        override fun onAdClicked() {
                            _adNativeLanguage2.value = null
                            preloadNativeAdLanguage2()
                        }
                    }
                )
            }
        }
    }

    private val _adNativeOnboard1Click = MutableStateFlow<NativeAdState?>(null)
    val adNativeOnboard1Click: StateFlow<NativeAdState?> = _adNativeOnboard1Click.asStateFlow()
    private val _createAdNativeOnboard1Click by lazy { MinSapNativeAd.create() }
    val createAdNativeOnboard1Click: MinSapNativeAd = _createAdNativeOnboard1Click

    fun getAdNativeOnboard1Click() = _adNativeOnboard1Click

    fun preloadNativeAdOnboard1Click() {
        if (firebaseRemoteConfig.getBoolean(RemoteConfigAdsConst.NATIVE_ONBOARDING_1_CLICK_SHOW)) {
            if (_adNativeOnboard1Click.value != null) return
            viewModelScope.launch(Dispatchers.IO) {
                createAdNativeOnboard1Click.loadNativeAdPriorityAlternate(
                    context = app,
                    adUnitIdPriority = AdsIdUtils.NATIVE_OB1_CLICK_HIGH,
                    adUnitIdNormal = AdsIdUtils.NATIVE_OB1_CLICK,
                    nameAdPriority = "High Onboard 1 Click",
                    nameAdNormal = "Onboard 1 Click",
                    callbackResult = object : NativeAdCallBack() {
                        override fun onAdLoaded(nativeAdValue: NativeAdValue) {
                            _adNativeOnboard1Click.value =
                                NativeAdState.NativeAdData(nativeAdValue = nativeAdValue)
                        }

                        override fun onAdFailedToLoad(error: AdError) {
                            _adNativeOnboard1Click.value =
                                NativeAdState.NativeAdError(adError = error)
                        }

                        override fun onAdClicked() {
                            preloadNativeAdOnboard1Click()
                        }
                    }
                )
            }
        }
    }

    private val _adNativeOnboard1 = MutableStateFlow<NativeAdState?>(null)
    val adNativeOnboard1: StateFlow<NativeAdState?> = _adNativeOnboard1.asStateFlow()
    private val _createAdNativeOnboard1 by lazy { MinSapNativeAd.create() }
    val createAdNativeOnboard1: MinSapNativeAd = _createAdNativeOnboard1

    fun getAdNativeOnboard1() = _adNativeOnboard1

    fun preloadNativeAdOnboard1() {
        if (firebaseRemoteConfig.getBoolean(RemoteConfigAdsConst.NATIVE_ONBOARDING_1_SHOW)) {
            if (_adNativeOnboard1.value != null) return
            viewModelScope.launch(Dispatchers.IO) {
                _createAdNativeOnboard1.loadNativeAdPriorityAlternate(
                    context = app,
                    adUnitIdPriority = AdsIdUtils.NATIVE_OB1_HIGH,
                    adUnitIdNormal = AdsIdUtils.NATIVE_OB1,
                    nameAdPriority = "High Onboard 1",
                    nameAdNormal = "Onboard 1",
                    callbackResult = object : NativeAdCallBack() {
                        override fun onAdLoaded(nativeAdValue: NativeAdValue) {
                            _adNativeOnboard1.value =
                                NativeAdState.NativeAdData(nativeAdValue = nativeAdValue)
                        }

                        override fun onAdFailedToLoad(error: AdError) {
                            _adNativeOnboard1.value = NativeAdState.NativeAdError(adError = error)
                        }

                        override fun onAdClicked() {
                            _adNativeOnboard1Click.value = null
                            preloadNativeAdOnboard1()
                        }
                    }
                )
            }
        }
    }

    private val _adNativeFull1 = MutableStateFlow<NativeAdState?>(null)
    val adNativeFull1: StateFlow<NativeAdState?> = _adNativeFull1.asStateFlow()
    private val _createAdNativeFull1 by lazy { MinSapNativeAd.create() }
    val createAdNativeFull1: MinSapNativeAd = _createAdNativeFull1

    fun getAdNativeFull1() = _adNativeFull1

    fun preloadNativeAdFull1() {
        if (firebaseRemoteConfig.getBoolean(RemoteConfigAdsConst.NATIVE_FULL_1_SHOW)) {
            if (_adNativeFull1.value != null) return
            viewModelScope.launch(Dispatchers.IO) {
                _createAdNativeFull1.loadNativeAdPriorityAlternate(
                    context = app,
                    adUnitIdPriority = AdsIdUtils.NATIVE_FULL1_HIGH,
                    adUnitIdNormal = AdsIdUtils.NATIVE_FULL1,
                    nameAdPriority = "High Full 1",
                    nameAdNormal = "Full 1",
                    callbackResult = object : NativeAdCallBack() {
                        override fun onAdLoaded(nativeAdValue: NativeAdValue) {
                            _adNativeFull1.value =
                                NativeAdState.NativeAdData(nativeAdValue = nativeAdValue)
                        }

                        override fun onAdFailedToLoad(error: AdError) {
                            _adNativeFull1.value = NativeAdState.NativeAdError(adError = error)
                        }

                        override fun onAdClicked() {
                            _adNativeFull1.value = null
                            preloadNativeAdFull1()
                        }
                    }
                )
            }
        }
    }

    private val _adNativeFull2 = MutableStateFlow<NativeAdState?>(null)
    val adNativeFull2: StateFlow<NativeAdState?> = _adNativeFull2.asStateFlow()
    private val _createAdNativeFull2 by lazy { MinSapNativeAd.create() }
    val createAdNativeFull2: MinSapNativeAd = _createAdNativeFull2

    fun getAdNativeFull2() = _adNativeFull2

    fun preloadNativeAdFull2() {
        if (firebaseRemoteConfig.getBoolean(RemoteConfigAdsConst.NATIVE_FULL_2_SHOW)) {
            if (_adNativeFull2.value != null) return
            viewModelScope.launch(Dispatchers.IO) {
                _createAdNativeFull2.loadNativeAdPriorityAlternate(
                    context = app,
                    adUnitIdPriority = AdsIdUtils.NATIVE_FULL2_HIGH,
                    adUnitIdNormal = AdsIdUtils.NATIVE_FULL2,
                    nameAdPriority = "High Full 2",
                    nameAdNormal = "Full 2",
                    callbackResult = object : NativeAdCallBack() {
                        override fun onAdLoaded(nativeAdValue: NativeAdValue) {
                            _adNativeFull2.value =
                                NativeAdState.NativeAdData(nativeAdValue = nativeAdValue)
                        }

                        override fun onAdFailedToLoad(error: AdError) {
                            _adNativeFull2.value = NativeAdState.NativeAdError(adError = error)
                        }

                        override fun onAdClicked() {
                            _adNativeFull2.value = null
                            preloadNativeAdFull2()
                        }
                    }
                )
            }
        }
    }
}