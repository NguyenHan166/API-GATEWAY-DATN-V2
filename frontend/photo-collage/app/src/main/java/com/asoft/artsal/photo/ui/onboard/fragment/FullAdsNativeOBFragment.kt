package com.asoft.artsal.photo.ui.onboard.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import com.artsal.photo.editor.collage.maker.R
import com.asoft.artsal.photo.base.BaseFragment
import com.artsal.photo.editor.collage.maker.databinding.AdsNativeFullBinding
import com.artsal.photo.editor.collage.maker.databinding.FragmentAdsFullNativeOnboardingBinding
import com.asoft.artsal.photo.extensions.collectIn
import com.asoft.artsal.photo.extensions.collectNativeAdData
import com.asoft.artsal.photo.extensions.getAdNativeFullBinding
import com.asoft.artsal.photo.extensions.loadImageGifFromDrawable
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.serializable
import com.asoft.artsal.photo.ui.ads.AdsOnboardingViewModel
import com.asoft.artsal.photo.ui.onboard.viewmodel.OnboardingViewModel
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.minsap.ad.ads.common.NativeAdState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FullAdsNativeOBFragment : BaseFragment<FragmentAdsFullNativeOnboardingBinding>() {
    @Inject
    lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    private var onboardingPageType: OnboardingPageType? = null
    private var adsBinding1: AdsNativeFullBinding? = null
    private var adsBinding2: AdsNativeFullBinding? = null
    private val onboardingViewModel by activityViewModels<OnboardingViewModel>()
    private val adsOnboardingViewModel by activityViewModels<AdsOnboardingViewModel>()

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAdsFullNativeOnboardingBinding =
        FragmentAdsFullNativeOnboardingBinding.inflate(inflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onboardingPageType = arguments?.serializable(TYPE_FULL_NATIVE)
    }

    override fun initListener() {
        binding?.ivClose?.onClick {
            onboardingViewModel.redirectNextScreen.call(true)
        }
    }

    override fun setupUi() {

    }

    override fun renderUi() {
        if (onboardingPageType == OnboardingPageType.AD1) {
            adsOnboardingViewModel.adNativeFull1.collectNativeAdData(
                this,
            ) { adNativeState ->
                when (adNativeState) {
                    is NativeAdState.NativeAdData -> {
                        adsBinding1 = null
                        adsBinding1 =
                            getAdNativeFullBinding()
                        adsOnboardingViewModel.createAdNativeFull1.showNativeAd(
                            frameAd = binding?.frameNative,
                            adsBinding = adsBinding1 ?: return@collectNativeAdData,
                            nativeAdValue = adNativeState.nativeAdValue,
                            subscribe = adsOnboardingViewModel.getAdNativeFull1(),
                            isConfigRatio = false,
                            isReload = false
                        )
                    }

                    is NativeAdState.NativeAdError -> {

                    }

                    else -> {}
                }
            }
        } else if (onboardingPageType == OnboardingPageType.AD2) {
            adsOnboardingViewModel.adNativeFull2.collectNativeAdData(
                this,
            ) { adNativeState ->
                when (adNativeState) {
                    is NativeAdState.NativeAdData -> {
                        adsBinding2 = null
                        adsBinding2 = getAdNativeFullBinding()
                        adsOnboardingViewModel.createAdNativeFull2.showNativeAd(
                            frameAd = binding?.frameNative,
                            adsBinding = adsBinding2 ?: return@collectNativeAdData,
                            nativeAdValue = adNativeState.nativeAdValue,
                            subscribe = adsOnboardingViewModel.getAdNativeFull2(),
                            isConfigRatio = false,
                            isReload = false
                        )
                    }

                    is NativeAdState.NativeAdError -> {

                    }

                    else -> {}
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding?.imgSwipe?.loadImageGifFromDrawable(
            drawableRes = R.drawable.img_swipe,
            overrideWidth = 100,
            overrideHeight = 100
        )
    }


    override fun onDestroyView() {
        adsBinding1 = null
        adsBinding2 = null
        binding?.imgSwipe?.setImageDrawable(null)
        super.onDestroyView()
    }

    override fun onDestroy() {
        adsOnboardingViewModel.run {
            createAdNativeFull1.destroy()
            createAdNativeFull2.destroy()
        }
        super.onDestroy()
    }

    companion object {
        private const val TYPE_FULL_NATIVE = "TYPE_FULL_NATIVE"

        fun newInstance(type: OnboardingPageType) = FullAdsNativeOBFragment().apply {
            arguments = Bundle().apply {
                putSerializable(TYPE_FULL_NATIVE, type)
            }
        }
    }
}