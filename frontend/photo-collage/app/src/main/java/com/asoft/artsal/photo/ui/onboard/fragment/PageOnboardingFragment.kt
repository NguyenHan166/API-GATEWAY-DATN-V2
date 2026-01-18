package com.asoft.artsal.photo.ui.onboard.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.fragment.app.activityViewModels
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.FragmentPageOnboardingBinding
import com.artsal.photo.editor.collage.maker.databinding.ItemObPage1Binding
import com.artsal.photo.editor.collage.maker.databinding.ItemObPage2Binding
import com.artsal.photo.editor.collage.maker.databinding.ItemObPage3Binding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.customview.HabitsView
import com.asoft.artsal.photo.extensions.collectNativeAdData
import com.asoft.artsal.photo.extensions.getAdNativeMediumRectangleBinding
import com.asoft.artsal.photo.extensions.internal.getAdsKey
import com.asoft.artsal.photo.extensions.invisible
import com.asoft.artsal.photo.extensions.loadImageDrawableWithCompress
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.toast
import com.asoft.artsal.photo.extensions.visible
import com.asoft.artsal.photo.ui.ads.AdsOnboardingViewModel
import com.asoft.artsal.photo.ui.onboard.viewmodel.OnboardingViewModel
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import com.asoft.artsal.photo.utils.RemoteConfigAdsConst
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.minsap.ad.ads.common.NativeAdState
import com.minsap.ad.config.NativeAdValue
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PageOnboardingFragment : BaseFragment<FragmentPageOnboardingBinding>() {
    @Inject
    lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    private val onboardingViewModel by activityViewModels<OnboardingViewModel>()
    private val adsOnboardingViewModel by activityViewModels<AdsOnboardingViewModel>()
    private var position: Int? = null
    private var bindingPage1: ItemObPage1Binding? = null
    private var bindingPage2: ItemObPage2Binding? = null
    private var bindingPage3: ItemObPage3Binding? = null
    private var checkFirstSelected: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        position = arguments?.getInt(KEY_BUNDLE_POSITION)
    }

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPageOnboardingBinding =
        FragmentPageOnboardingBinding.inflate(inflater, container, false)

    override fun initListener() {
        // Empty //
    }

    override fun setupUi() {
        when (position) {
            0 -> {
                bindingPage1 =
                    ItemObPage1Binding.inflate(layoutInflater, binding?.rootOnboarding, true)
                bindingPage1?.run {
                    tvNext.onClick {
                        FirebaseEventUtils.logEventTracking(context, "onboarding_1_click_next")

                        if (tvNext.alpha == 1f){
                            redirectToNextPage()
                        } else {
                            toast(getString(R.string.please_select_an_option_to_continue))
                        }
                    }
                    gvHabits.forEach { habitView ->
                        habitView.setOnClickListener {
                            FirebaseEventUtils.logEventTracking(context, "onboarding_1_select")
                            selectedHabit(habitView as HabitsView)
                        }
                    }
                }
            }

            1 -> {
                bindingPage2 =
                    ItemObPage2Binding.inflate(layoutInflater, binding?.rootOnboarding, true)
                bindingPage2?.run {
                    imgBg.loadImageDrawableWithCompress(drawableRes = R.drawable.img_onboarding2)
                    btnNext.onClick {
                        FirebaseEventUtils.logEventTracking(context, "onboarding_2_click_next")

                        redirectToNextPage()
                    }
                }
            }

            2, 3 -> {
                bindingPage3 =
                    ItemObPage3Binding.inflate(layoutInflater, binding?.rootOnboarding, true)
                bindingPage3?.run {
                    imgBg.loadImageDrawableWithCompress(drawableRes = R.drawable.img_onboarding3)
                    btnNext.onClick {
                        FirebaseEventUtils.logEventTracking(context, "onboarding_3_click_next")

                        redirectToNextPage()
                    }
                }
            }
        }
    }

    private fun selectedHabit(habitView: HabitsView) {
        Timber.i("Selected habit: ${habitView.tag}")

        habitView.setSelectState(!habitView.getSelectState())
        checkingNextView()

        if (checkFirstSelected) return
        checkFirstSelected = true
        if (firebaseRemoteConfig.getAdsKey(
                context,
                RemoteConfigAdsConst.NATIVE_ONBOARDING_1_CLICK_SHOW
            )
        ) {
            adsOnboardingViewModel.adNativeOnboard1Click.collectNativeAdData(this) { adNativeState ->
                when (adNativeState) {
                    is NativeAdState.NativeAdData -> {
                        showAdOnboard1Click(adNativeState.nativeAdValue)
                    }

                    is NativeAdState.NativeAdError -> {
                    }

                    else -> {}
                }
            }
        } else {
            bindingPage1?.frameNative?.root?.invisible()
        }

        adsOnboardingViewModel.preloadNativeAdFull1()
    }

    private fun checkingNextView() {
        Timber.i("Checking next able view...  ")
        bindingPage1?.run {
            gvHabits.forEach { habitView ->
                if ((habitView as? HabitsView)?.getSelectState() == true) {
                    tvNext.alpha = 1f
                    return
                }
            }
            tvNext.alpha = 0.3f

        }
    }

    private fun showAdOnboard1Click(nativeAdValue: NativeAdValue) {
        Timber.i("Showing ads for Onboarding 1 click habit...")

        val adsBinding = getAdNativeMediumRectangleBinding()
        adsOnboardingViewModel.createAdNativeOnboard1Click.showNativeAd(
            frameAd = bindingPage1?.frameNative?.root,
            adsBinding = adsBinding,
            nativeAdValue = nativeAdValue,
            subscribe = adsOnboardingViewModel.getAdNativeOnboard1Click(),
            isConfigRatio = true,
            isReload = true,
            timDelay = 50L
        )
    }

    private fun redirectToNextPage() {
        onboardingViewModel.redirectNextScreen.call(true)
    }

    override fun renderUi() {
        if (position == 0) showAdOnboard1()
    }

    private fun showAdOnboard1() {
        if (firebaseRemoteConfig.getAdsKey(
                context,
                RemoteConfigAdsConst.NATIVE_ONBOARDING_1_SHOW
            )
        ) {
            bindingPage1?.frameNative?.root?.visible()
            adsOnboardingViewModel.adNativeOnboard1.collectNativeAdData(this) { adNativeState ->
                when (adNativeState) {
                    is NativeAdState.NativeAdData -> {
                        val adsBinding = getAdNativeMediumRectangleBinding()
                        adsOnboardingViewModel.createAdNativeOnboard1.showNativeAd(
                            frameAd = bindingPage1?.frameNative?.root,
                            adsBinding = adsBinding,
                            nativeAdValue = adNativeState.nativeAdValue,
                            subscribe = adsOnboardingViewModel.getAdNativeOnboard1(),
                            isConfigRatio = true,
                            isReload = true,
                        )
                    }

                    is NativeAdState.NativeAdError -> {
                        bindingPage1?.frameNative?.root?.invisible()
                    }

                    else -> {}
                }
            }
        }
    }

    companion object {
        private const val KEY_BUNDLE_POSITION = "KEY_BUNDLE_POSITION"

        fun newInstance(position: Int) =
            PageOnboardingFragment().apply {
                arguments = Bundle(1).apply {
                    putInt(KEY_BUNDLE_POSITION, position)
                }
            }
    }

    override fun onResume() {
        super.onResume()
        when (position) {
            0 -> {
                FirebaseEventUtils.logEventTracking(context, "onboarding_1_view")

                adsOnboardingViewModel.preloadNativeAdOnboard1Click()
            }

            1 -> {
                FirebaseEventUtils.logEventTracking(context, "onboarding_2_view")

                adsOnboardingViewModel.preloadNativeAdFull2()
            }

            2, 3 -> {
                FirebaseEventUtils.logEventTracking(context, "onboarding_3_view")

            }
        }
    }

    override fun onDestroyView() {
        bindingPage1 = null
        bindingPage2 = null
        bindingPage3 = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        adsOnboardingViewModel.run {
            createAdNativeOnboard1.destroy()
            createAdNativeOnboard1Click.destroy()
        }
        super.onDestroy()
    }
}