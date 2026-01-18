package com.asoft.artsal.photo.ui.onboard.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.asoft.artsal.photo.base.BaseFragment
import com.artsal.photo.editor.collage.maker.databinding.FragmentOnboardingBinding
import com.asoft.artsal.photo.extensions.internal.getAdsKey
import com.asoft.artsal.photo.ui.onboard.adapter.OnboardingVPAdapter
import com.asoft.artsal.photo.ui.onboard.viewmodel.OnboardingViewModel
import com.asoft.artsal.photo.utils.RemoteConfigAdsConst
import com.asoft.artsal.photo.utils.SharePreference
import dagger.hilt.android.AndroidEntryPoint
import java.io.Serializable
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingFragment : BaseFragment<FragmentOnboardingBinding>() {
    override val isInsets: Boolean
        get() = false
    private val onboardingViewModel by activityViewModels<OnboardingViewModel>()

    @Inject
    lateinit var sharePreference: SharePreference

    @Inject
    lateinit var firebaseRemoteConfig: FirebaseRemoteConfig

    private var onboardingVPAdapter: OnboardingVPAdapter? = null

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentOnboardingBinding = FragmentOnboardingBinding.inflate(inflater, container, false)

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            when (onboardingVPAdapter?.pages?.get(position)) {
                OnboardingPageType.OB -> {
                    binding?.vpOnboarding?.isUserInputEnabled = false
                }

                OnboardingPageType.AD1, OnboardingPageType.AD2 -> {
                    binding?.vpOnboarding?.isUserInputEnabled = true
                }

                OnboardingPageType.WELCOME -> {
                    binding?.vpOnboarding?.isUserInputEnabled = false
                }

                else -> {
                    binding?.vpOnboarding?.isUserInputEnabled = false
                }
            }
        }
    }

    override fun initListener() {
        binding?.vpOnboarding?.registerOnPageChangeCallback(pageChangeCallback)
    }

    override fun setupUi() {
        val isShowFullAd1 = firebaseRemoteConfig.getAdsKey(
            context,
            RemoteConfigAdsConst.NATIVE_FULL_1_SHOW
        )
        val isShowFullAd2 = firebaseRemoteConfig.getAdsKey(
            context,
            RemoteConfigAdsConst.NATIVE_FULL_2_SHOW
        )
        onboardingVPAdapter = OnboardingVPAdapter(
            childFragmentManager,
            viewLifecycleOwner.lifecycle,
            isShowFullAd1,
            isShowFullAd2,
        )
        binding?.vpOnboarding?.adapter = onboardingVPAdapter
        binding?.vpOnboarding?.offscreenPageLimit = 1
    }

    override fun renderUi() {
        onboardingViewModel.redirectNextScreen.observe(viewLifecycleOwner) {
            onNext()
        }
    }

    private fun onNext() {
        binding?.vpOnboarding?.currentItem = binding?.vpOnboarding?.currentItem?.plus(1) ?: 0
    }

    override fun onDestroyView() {
        binding?.vpOnboarding?.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onDestroyView()
    }
}

enum class OnboardingPageType : Serializable {
    OB, AD1, AD2, WELCOME
}