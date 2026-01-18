package com.asoft.artsal.photo.ui.splash.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.AdsNativeMediumNoMediaRectangleBinding
import com.artsal.photo.editor.collage.maker.databinding.FragmentSplashBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.event.AppEventProvider
import com.asoft.artsal.photo.extensions.collectNativeAdData
import com.asoft.artsal.photo.extensions.internal.getAdsKey
import com.asoft.artsal.photo.extensions.invisible
import com.asoft.artsal.photo.extensions.isAtLeastStarted
import com.asoft.artsal.photo.extensions.launchAndRepeat
import com.asoft.artsal.photo.extensions.loadImageDrawableWithCompress
import com.asoft.artsal.photo.extensions.navigateToWithAnim
import com.asoft.artsal.photo.extensions.visible
import com.asoft.artsal.photo.ui.ads.AdsHomeViewModel
import com.asoft.artsal.photo.ui.ads.AdsOnboardingViewModel
import com.asoft.artsal.photo.ui.home.viewmodel.HomeViewModel
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import com.asoft.artsal.photo.utils.RemoteConfigAdsConst
import com.asoft.artsal.photo.utils.SharePreference
import com.asoft.artsal.photo.utils.SingleLiveEvent
import com.asoft.artsal.photo.utils.TimerListener
import com.asoft.artsal.photo.utils.WrapperCountDownTimer
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.minsap.ad.ads.MinSapInterstitialAd
import com.minsap.ad.ads.common.InterstitialAdState
import com.minsap.ad.ads.common.NativeAdState
import com.minsap.ad.config.AdError
import com.minsap.ad.config.NativeAdValue
import com.minsap.ad.consent.ConsentUMP
import com.minsap.ad.listener.InterstitialAdCallBack
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SplashFragment : BaseFragment<FragmentSplashBinding>() {
    override val isInsets: Boolean
        get() = false

    @Inject
    lateinit var sharePreference: SharePreference

    @Inject
    lateinit var firebaseRemoteConfig: FirebaseRemoteConfig

    @Inject
    lateinit var consentUMP: ConsentUMP

    private val homeViewModel by activityViewModels<HomeViewModel>()
    private val adsHomeViewModel by activityViewModels<AdsHomeViewModel>()

    private val adsOnboardingViewModel by activityViewModels<AdsOnboardingViewModel>()
    val isFirstOpenApp by lazy {
        sharePreference.get<Boolean>(
            SharePreference.IS_FIRST_OPEN_APP,
            true
        )
    }
    private var countDownTimer: WrapperCountDownTimer? = null

    private var isStartCountDown = false

    private val redirectEvent = SingleLiveEvent<AppEventProvider>()

    private var timeRemain: Long = 0L

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSplashBinding = FragmentSplashBinding.inflate(inflater, container, false)

    override fun initListener() {
        // Nothing
    }

    override fun setupUi() {
        FirebaseEventUtils.logEventTracking(
            context,
            if (isFirstOpenApp == true) "splash_view_1" else "splash_view_2"
        )

        binding?.imgBg?.loadImageDrawableWithCompress(R.drawable.img_bg_splash)
    }

    override fun renderUi() {
        try {
            consentUMP.requestConsentAndInitializeAds(activity ?: return) {
                countDownTime()
                loadAdSplash()
            }
        } catch (ex: Exception) {
            FirebaseEventUtils.recordException(ex)
            redirectNextScreen()
        }

        launchAndRepeat {
            adsOnboardingViewModel.adInterHighSplash.observe(viewLifecycleOwner) { interstitialAdState ->
                if (firebaseRemoteConfig.getAdsKey(
                        context,
                        RemoteConfigAdsConst.INTER_SPLASH_SHOW
                    )
                ) {
                    when (interstitialAdState) {
                        is InterstitialAdState.InterstitialAdData -> {
                            showInterAd(
                                minsapInterstitialAd = adsOnboardingViewModel.createInterHighSplash,
                                interstitialAd = interstitialAdState.interstitialAdValue.getAdValue()
                                    ?: return@observe
                            )
                        }

                        is InterstitialAdState.InterstitialAdError -> {
                            if (timeRemain >= 5000L) {
                                adsOnboardingViewModel.preloadInterAdSplash()
                            }
                        }

                        else -> {}
                    }
                }
            }
        }

        launchAndRepeat {
            adsOnboardingViewModel.adInterSplash.observe(viewLifecycleOwner) { interstitialAdState ->
                when (interstitialAdState) {
                    is InterstitialAdState.InterstitialAdData -> {
                        showInterAd(
                            minsapInterstitialAd = adsOnboardingViewModel.createInterSplash,
                            interstitialAd = interstitialAdState.interstitialAdValue.getAdValue()
                                ?: return@observe
                        )
                    }

                    is InterstitialAdState.InterstitialAdError -> {

                    }

                    else -> {}
                }
            }
        }

        homeViewModel.initDataAsset()
    }

    private fun showInterAd(
        minsapInterstitialAd: MinSapInterstitialAd,
        interstitialAd: InterstitialAd
    ) {
        minsapInterstitialAd.showInterstitialAd(
            context = activity,
            interstitialAd = interstitialAd,
            callBackShow = object : InterstitialAdCallBack() {
                override fun onAdDismissed() {
                    Timber.tag("SPLASH_DEBUG").d("onAdDismissed")
                    resumeCountDown()
                    redirectEvent.call(AppEventProvider.RedirectScreen)
                }

                override fun onAdFailedToShow(error: AdError?) {

                }

                override fun onAdShown() {
                    countDownTimer?.stop()
                    if (isFirstOpenApp == true) {
                        adsOnboardingViewModel.preloadNativeAdLanguage2()
                    }
                }
            }
        )
    }

    private fun loadAdSplash() {
        if (!isAdded) return
        adsOnboardingViewModel.preloadAdNativeBannerSplash()
        loadBannerAds()
        loadPreInterAds()
        if (isFirstOpenApp == true) {
            adsOnboardingViewModel.preloadNativeAdLanguage1()
        } else {
            adsHomeViewModel.run {
                preloadNativeHome()
            }
        }
    }

    private fun loadPreInterAds() {
        Timber.tag(TAG).i("loadPreInterAds...")
        adsOnboardingViewModel.preloadInterHighAdSplash()
    }

    private fun loadBannerAds() {
        Timber.tag("SPLASH_DEBUG").d("loadBannerAds...")
        if (firebaseRemoteConfig.getAdsKey(context, RemoteConfigAdsConst.BANNER_SPLASH_SHOW)) {
            adsOnboardingViewModel.adNativeBannerSplash.collectNativeAdData(this) { adNativeState ->
                Timber.d("Collect Language 1 ==> $adNativeState")
                binding?.frameBanner?.root?.visible()
                when (adNativeState) {
                    is NativeAdState.NativeAdData -> {
                        showAdNativeBanner(adNativeState.nativeAdValue)
                    }

                    is NativeAdState.NativeAdError -> {
                        binding?.frameBanner?.root?.invisible()
                    }

                    else -> {}
                }
            }
        }

    }

    private fun showAdNativeBanner(nativeAdValue: NativeAdValue) {
        Timber.i("show Ad Native Banner ads...")
        val adsBinding1 = AdsNativeMediumNoMediaRectangleBinding.inflate(
            layoutInflater
        )
        adsOnboardingViewModel.createAdNativeBannerSplash.showNativeAd(
            frameAd = binding?.frameBanner?.root,
            adsBinding = adsBinding1,
            nativeAdValue = nativeAdValue,
            subscribe = adsOnboardingViewModel.getAdNativeBannerSplash(),
            isConfigRatio = false,
            isReload = false
        )
    }

    private fun redirectNextScreen() {
        if (!isAdded || activity == null) {
            return
        }
        Timber.tag("SPLASH_DEBUG").d("redirectNextScreen")
        if (isAtLeastStarted()) {

            if (isFirstOpenApp == true) {
                adsOnboardingViewModel.preloadNativeAdLanguage2()
                navigateToWithAnim(
                    id = R.id.action_splash_to_select_language,
                    popUpToId = R.id.splash_fragment,
                    isInclusive = true
                )
            } else {
                navigateToWithAnim(
                    id = R.id.action_splash_fragment_to_home_fragment,
                    popUpToId = R.id.splash_fragment,
                    isInclusive = true
                )
            }
        }
    }

    private fun countDownTime() {
        countDownTimer?.stop()
        countDownTimer = createCountDownTimer(TIME_SPLASH_LOADING)
        countDownTimer?.start()
    }

    private fun createCountDownTimer(leftMillis: Long): WrapperCountDownTimer {
        Timber.tag(TAG).i("createCountDownTimer...")
        return WrapperCountDownTimer(leftMillis, object : TimerListener {
            override fun onStart(startTime: Long) {
                isStartCountDown = true
            }

            override fun onTick(millisUntilFinished: Long) {
                timeRemain = millisUntilFinished
            }

            override fun onStop() {
                isStartCountDown = false
            }

            override fun onFinish() {
                redirectNextScreen()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        Timber.tag("SPLASH_DEBUG").d("onResume")
        redirectEvent.observe(viewLifecycleOwner) {
            if (it is AppEventProvider.RedirectScreen) {
                Timber.tag("SPLASH_DEBUG").d("Redirect Event")
                redirectNextScreen()
            }
        }
        resumeCountDown()
    }

    private fun resumeCountDown() {
        if (!isStartCountDown && timeRemain > 0L) {
            countDownTimer = null
            countDownTimer = createCountDownTimer(timeRemain)
            countDownTimer?.start()
        }
    }

    override fun onStop() {
        super.onStop()
        countDownTimer?.stop()
    }

    override fun onDestroyView() {
        countDownTimer = null
        binding?.lottieProgress?.cancelAnimation()
        binding?.lottieProgress?.clearAnimation()
        super.onDestroyView()
    }

    override fun onDestroy() {
        adsOnboardingViewModel.createAdNativeBannerSplash.destroy()
        adsOnboardingViewModel.createInterHighSplash.destroy()
        adsOnboardingViewModel.createInterSplash.destroy()
        super.onDestroy()
    }

    companion object {
        private const val TIME_SPLASH_LOADING = 20000L
        private const val TAG = "SPLASH_DEBUG"
    }

}