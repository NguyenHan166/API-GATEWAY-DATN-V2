package com.asoft.artsal.photo.ui.onboard.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.FragmentSelectLanguageBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.component.LinearSpacingItemDecoration
import com.asoft.artsal.photo.extensions.collectNativeAdData
import com.asoft.artsal.photo.extensions.dpToPx
import com.asoft.artsal.photo.extensions.getAdNativeMediumRectangleBinding
import com.asoft.artsal.photo.extensions.gone
import com.asoft.artsal.photo.extensions.initRecyclerViewAdapter
import com.asoft.artsal.photo.extensions.internal.getAdsKey
import com.asoft.artsal.photo.extensions.invisible
import com.asoft.artsal.photo.extensions.navigateToWithAnim
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.visible
import com.asoft.artsal.photo.ui.ads.AdsOnboardingViewModel
import com.asoft.artsal.photo.ui.onboard.adapter.SelectLanguageAdapter
import com.asoft.artsal.photo.ui.onboard.viewmodel.OnboardingViewModel
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import com.asoft.artsal.photo.utils.RemoteConfigAdsConst
import com.asoft.artsal.photo.utils.SharePreference
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.minsap.ad.ads.common.NativeAdState
import com.minsap.ad.config.NativeAdValue
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import kotlin.text.get

@AndroidEntryPoint
class SelectLanguageFragment : BaseFragment<FragmentSelectLanguageBinding>() {

    @Inject
    lateinit var firebaseRemoteConfig: FirebaseRemoteConfig

    @Inject
    lateinit var sharePreference: SharePreference
    private val onboardingViewModel by activityViewModels<OnboardingViewModel>()
    private val adsOnboardingViewModel by activityViewModels<AdsOnboardingViewModel>()
    private val selectLanguageAdapter by lazy { SelectLanguageAdapter(::onSelectLanguage) }


    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSelectLanguageBinding =
        FragmentSelectLanguageBinding.inflate(inflater, container, false)

    override fun initListener() {
        Timber.i("Setup listener...")

        binding?.btnNext?.onClick {
            FirebaseEventUtils.logEventTracking(context, "lfo_click_next")

            val languageCode = onboardingViewModel.codeLanguage.value
            if (!languageCode.isNullOrEmpty()) {
                sharePreference.save(SharePreference.LANGUAGE_APP, languageCode)
                activity?.recreate()
            }
            navigateToWithAnim(id = R.id.action_select_language_to_onboarding)
        }
    }

    override fun setupUi() {
        Timber.i("Setup Ui...")
        FirebaseEventUtils.logEventTracking(context, "lfo_view")

        binding?.rvLanguages?.apply {
            initRecyclerViewAdapter(
                yourAdapter = selectLanguageAdapter,
                yourLayoutManager = LinearLayoutManager(
                    requireContext(),
                    LinearLayoutManager.VERTICAL,
                    false
                )
            )
            addItemDecoration(
                LinearSpacingItemDecoration(
                    isHorizontal = false,
                    spacing = 16.dpToPx()
                )
            )
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    if (binding?.btnNext?.isVisible == true) return
                    if (firstVisibleItemPosition > 0) {
                        binding?.lottieSelect?.gone()
                    } else {
                        binding?.lottieSelect?.visible()
                    }
                }
            })

            if (sharePreference.get<String>(SharePreference.LANGUAGE_APP).orEmpty().isNotEmpty()) {
                binding?.lottieSelect?.setImageDrawable(null)
                binding?.btnNext?.visible()
            }
        }
    }

    override fun renderUi() {
        Timber.i("render UI...")
        onboardingViewModel.getLanguages()

        onboardingViewModel.languages.observe(viewLifecycleOwner) {
            selectLanguageAdapter.submitList(it)
        }

        if (firebaseRemoteConfig.getAdsKey(context, RemoteConfigAdsConst.NATIVE_LANGUAGE_1_SHOW)) {
            adsOnboardingViewModel.adNativeLanguage1.collectNativeAdData(this) { adNativeState ->
                Timber.d("Collect Language 1 ==> $adNativeState")
                binding?.frameNative?.root?.visible()
                when (adNativeState) {
                    is NativeAdState.NativeAdData -> {

                        showAdNative1(adNativeState.nativeAdValue)
                    }

                    is NativeAdState.NativeAdError -> {
                        binding?.frameNative?.root?.invisible()
                    }

                    else -> {}
                }
            }
        }
    }

    private fun showAdNative1(nativeAdValue: NativeAdValue) {
        Timber.i("show Ad Native 1...")
        val adsBinding1 = getAdNativeMediumRectangleBinding()
        adsOnboardingViewModel.createAdNativeLanguage1.showNativeAd(
            frameAd = binding?.frameNative?.root,
            adsBinding = adsBinding1,
            nativeAdValue = nativeAdValue,
            subscribe = adsOnboardingViewModel.getAdNativeLanguage1(),
            isConfigRatio = true,
            isReload = false
        )
    }


    private fun onSelectLanguage(code: String) {
        Timber.i("on select language: $code")
        FirebaseEventUtils.logEventTracking(context, "lfo_click_language")

        onboardingViewModel.selectLanguage(code)
        binding?.lottieSelect?.gone()

        if (binding?.btnNext?.isVisible == true) return
        if (firebaseRemoteConfig.getAdsKey(context, RemoteConfigAdsConst.NATIVE_LANGUAGE_2_SHOW)) {
            adsOnboardingViewModel.adNativeLanguage2.collectNativeAdData(this) { adNativeState ->
                when (adNativeState) {
                    is NativeAdState.NativeAdData -> {
                        showAdNative2(adNativeState.nativeAdValue)
                    }

                    is NativeAdState.NativeAdError -> {
                    }

                    else -> {}
                }
            }
        } else {
            binding?.frameNative?.root?.invisible()
        }

        binding?.btnNext?.visible()

        Timber.i("preload Native AdOnboard1Click...")
    }

    private fun showAdNative2(nativeAdValue: NativeAdValue) {
        Timber.i("show AdNative2...")
        val adsBinding2 = getAdNativeMediumRectangleBinding()
        adsOnboardingViewModel.createAdNativeLanguage2.showNativeAd(
            frameAd = binding?.frameNative?.root,
            adsBinding = adsBinding2,
            nativeAdValue = nativeAdValue,
            subscribe = adsOnboardingViewModel.getAdNativeLanguage2(),
            isConfigRatio = true,
            isReload = false
        )
    }


    override fun onResume() {
        super.onResume()
        adsOnboardingViewModel.preloadNativeAdOnboard1()
    }

    override fun onDestroyView() {
        binding?.lottieSelect?.cancelAnimation()
        binding?.lottieSelect?.clearAnimation()
        super.onDestroyView()
    }

    override fun onDestroy() {
        adsOnboardingViewModel.createAdNativeLanguage1.destroy()
        adsOnboardingViewModel.createAdNativeLanguage2.destroy()
        super.onDestroy()
    }
}