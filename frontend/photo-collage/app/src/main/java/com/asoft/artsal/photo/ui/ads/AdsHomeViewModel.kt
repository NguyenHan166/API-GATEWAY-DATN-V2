package com.asoft.artsal.photo.ui.ads

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.asoft.artsal.photo.base.BaseViewModel
import com.asoft.artsal.photo.event.AppEventProvider
import com.asoft.artsal.photo.extensions.getNativeAdData
import com.asoft.artsal.photo.utils.AdsIdUtils
import com.asoft.artsal.photo.utils.RemoteConfigAdsConst
import com.asoft.artsal.photo.utils.SharePreference
import com.asoft.artsal.photo.utils.SingleLiveEvent
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.minsap.ad.ads.MinSapBannerAd
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AdsHomeViewModel @Inject constructor(
    override val app: Application,
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
    private val sharePreference: SharePreference,
) : BaseViewModel(app = app) {
    private var isFirstOpen: Boolean = true

    init {
        isFirstOpen = sharePreference.get<Boolean>(SharePreference.IS_FIRST_OPEN_APP, true) ?: true
    }

    ///////////////////////////////////////////////////////////////////
    //                  ADS Banner
    //////////////////////////////////////////////////////////////////
    val createBannerAdEditCollage by lazy { MinSapBannerAd.create() }
    val createBannerAdEditTemplate by lazy { MinSapBannerAd.create() }
    val createBannerGalleryPhoto by lazy { MinSapBannerAd.create() }





    ///////////////////////////////////////////////////////////////////
    //                  ADS Inter Save Collage
    //////////////////////////////////////////////////////////////////
    val isLoadingStartSaveDialog = SingleLiveEvent<Boolean>()
    private val _adInterSaveCollage = SingleLiveEvent<InterstitialAdState?>()
    val adInterSaveCollage: LiveData<InterstitialAdState?> = _adInterSaveCollage
    private val _createInterCollage by lazy { MinSapInterstitialAd.create() }
    val createInterCollage: MinSapInterstitialAd = _createInterCollage

    val redirectSaveResultEvent = SingleLiveEvent<AppEventProvider>()

    fun getAdSaveCollageResult() = _adInterSaveCollage

    fun preloadInterAdSaveCollage() {
        if (firebaseRemoteConfig.getBoolean(RemoteConfigAdsConst.INTER_SAVE_COLLAGE)) {
            if (_adInterSaveCollage.value != null) return
            isLoadingStartSaveDialog.call(true)
            _createInterCollage.loadInterstitialAdResult(
                context = app,
                adUnitId = AdsIdUtils.INTER_SAVE_COLLAGE,
                nameAd = "SaveCollage",
                callBackResult = object : InterstitialAdCallBack() {
                    override fun onAdLoaded(ad: InterstitialAdValue) {
                        super.onAdLoaded(ad)
                        isLoadingStartSaveDialog.call(false)
                        _adInterSaveCollage.call(
                            InterstitialAdState.InterstitialAdData(
                                interstitialAdValue = ad
                            )
                        )
                    }

                    override fun onAdFailedToLoad(error: AdError) {
                        super.onAdFailedToLoad(error)
                        isLoadingStartSaveDialog.call(false)
                        _adInterSaveCollage.call(
                            InterstitialAdState.InterstitialAdError(
                                adError = error
                            )
                        )
                    }
                }
            )
        } else {
            redirectSaveResultEvent.call(AppEventProvider.RedirectSavedResultScreen)
        }
    }

    ///////////////////////////////////////////////////////////////////
    //                  ADS Inter Save Template
    //////////////////////////////////////////////////////////////////
    val isLoadingSaveTemplateDialog = SingleLiveEvent<Boolean>()
    private val _adInterSaveTemplate = SingleLiveEvent<InterstitialAdState?>()
    val adInterSaveTemplate: LiveData<InterstitialAdState?> = _adInterSaveTemplate
    private val _createInterTemplate by lazy { MinSapInterstitialAd.create() }
    val createInterTemplate: MinSapInterstitialAd = _createInterTemplate

    val redirectSaveTemplateEvent = SingleLiveEvent<AppEventProvider>()

    fun getAdSaveTemplateResult() = _adInterSaveTemplate

    fun preloadInterAdSaveTemplate() {
        if (firebaseRemoteConfig.getBoolean(RemoteConfigAdsConst.INTER_SAVE_TEMPLATE)) {
            if (_adInterSaveTemplate.value != null) return
            isLoadingSaveTemplateDialog.call(true)
            _createInterTemplate.loadInterstitialAdResult(
                context = app,
                adUnitId = AdsIdUtils.INTER_SAVE_TEMPLATE,
                nameAd = "Save Template",
                callBackResult = object : InterstitialAdCallBack() {
                    override fun onAdLoaded(ad: InterstitialAdValue) {
                        super.onAdLoaded(ad)
                        isLoadingSaveTemplateDialog.call(false)
                        _adInterSaveTemplate.call(
                            InterstitialAdState.InterstitialAdData(
                                interstitialAdValue = ad
                            )
                        )
                    }

                    override fun onAdFailedToLoad(error: AdError) {
                        super.onAdFailedToLoad(error)
                        isLoadingSaveTemplateDialog.call(false)
                        _adInterSaveTemplate.call(
                            InterstitialAdState.InterstitialAdError(
                                adError = error
                            )
                        )
                    }
                }
            )
        } else {
            redirectSaveTemplateEvent.call(AppEventProvider.RedirectSavedResultScreen)
        }
    }

    ///////////////////////////////////////////////////////////////////
    //                  ADS Native Save
    //////////////////////////////////////////////////////////////////

    private val _adNativeSaveImage = MutableStateFlow<NativeAdState?>(null)
    val adNativeSaveImage: StateFlow<NativeAdState?> = _adNativeSaveImage.asStateFlow()
    private val _createAdNativeSaveImage by lazy { MinSapNativeAd.create() }
    val createAdNativeSaveImage: MinSapNativeAd = _createAdNativeSaveImage

    fun getAdNativeSaveImage() = _adNativeSaveImage

    fun preloadNativeAdSaveImage() {
        if(firebaseRemoteConfig.getBoolean(RemoteConfigAdsConst.NATIVE_RESULT_SHOW)) {
            if (_adNativeSaveImage.getNativeAdData() != null) return
            viewModelScope.launch(Dispatchers.IO) {
                _createAdNativeSaveImage.loadNativeAdResult(
                    context = app,
                    adUnitId = AdsIdUtils.NATIVE_RESULT,
                    nameAd = "Result",
                    callbackResult = object : NativeAdCallBack() {
                        override fun onAdLoaded(nativeAdValue: NativeAdValue) {
                            _adNativeSaveImage.value =
                                NativeAdState.NativeAdData(nativeAdValue = nativeAdValue)
                        }

                        override fun onAdFailedToLoad(error: AdError) {
                            _adNativeSaveImage.value = NativeAdState.NativeAdError(adError = error)
                        }

                        override fun onAdClicked() {
                            preloadNativeAdSaveImage()
                        }
                    }
                )
            }
        }
    }

    ///////////////////////////////////////////////////////////////////
    //                  ADS Native Home
    //////////////////////////////////////////////////////////////////
    private val _adNativeHome = MutableStateFlow<NativeAdState?>(null)
    val adNativeHome: StateFlow<NativeAdState?> = _adNativeHome.asStateFlow()
    private val _createAdNativeHome by lazy { MinSapNativeAd.create() }
    val createAdNativeHome: MinSapNativeAd = _createAdNativeHome

    fun getAdNativeHome() = _adNativeHome

    fun preloadNativeHome() {
        if(firebaseRemoteConfig.getBoolean(RemoteConfigAdsConst.NATIVE_HOME_SHOW)) {
            if (_adNativeHome.getNativeAdData() != null) return
            viewModelScope.launch(Dispatchers.IO) {
                _createAdNativeHome.loadNativeAdResult(
                    context = app,
                    adUnitId = AdsIdUtils.NATIVE_HOME,
                    nameAd = "HOME",
                    callbackResult = object : NativeAdCallBack() {
                        override fun onAdLoaded(nativeAdValue: NativeAdValue) {
                            _adNativeHome.value =
                                NativeAdState.NativeAdData(nativeAdValue = nativeAdValue)
                        }

                        override fun onAdFailedToLoad(error: AdError) {
                            _adNativeHome.value = NativeAdState.NativeAdError(adError = error)
                        }

                        override fun onAdClicked() {
                            preloadNativeHome()
                        }
                    }
                )
            }
        }
    }
}