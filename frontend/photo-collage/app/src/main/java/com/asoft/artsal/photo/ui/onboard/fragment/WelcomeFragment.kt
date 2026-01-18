package com.asoft.artsal.photo.ui.onboard.fragment

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.artsal.photo.editor.collage.maker.R
import com.asoft.artsal.photo.base.BaseFragment
import com.artsal.photo.editor.collage.maker.databinding.FragmentWelcomeBinding
import com.asoft.artsal.photo.extensions.loadImageDrawableWithCompress
import com.asoft.artsal.photo.extensions.navigateToWithAnim
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.openBrowser
import com.asoft.artsal.photo.ui.ads.AdsHomeViewModel
import com.asoft.artsal.photo.ui.home.viewmodel.HomeViewModel
import com.asoft.artsal.photo.utils.Const
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import com.asoft.artsal.photo.utils.SharePreference
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WelcomeFragment : BaseFragment<FragmentWelcomeBinding>() {
    override val isInsets: Boolean
        get() = false

    @Inject
    lateinit var sharePreference: SharePreference

    @Inject
    lateinit var firebaseRemoteConfig: FirebaseRemoteConfig

    private val adsHomeViewModel by activityViewModels<AdsHomeViewModel>()

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentWelcomeBinding = FragmentWelcomeBinding.inflate(inflater, container, false)

    override fun initListener() {
        binding?.run {
            btnStarted.onClick {
                FirebaseEventUtils.logEventTracking(context, "welcome_click_started")

                val isFirstOpenApp =
                    sharePreference.get<Boolean>(SharePreference.IS_FIRST_OPEN_APP, true)
                if (isFirstOpenApp == true) {
                    sharePreference.save(SharePreference.IS_FIRST_OPEN_APP, false)
                    navigateToWithAnim(
                        id = R.id.action_onboarding_fragment_to_home_fragment,
                        popUpToId = R.id.onboarding_fragment,
                        isInclusive = true
                    )
                }
            }

            tvPrivacySecond.onClick {
                context?.openBrowser(Const.LINK_PRIVACY)
            }
        }

    }

    override fun setupUi() {
        binding?.run {
            imgBg.loadImageDrawableWithCompress(drawableRes = R.drawable.img_welcome)

            tvPrivacySecond.paintFlags =
                binding?.tvPrivacySecond?.paintFlags?.or(Paint.UNDERLINE_TEXT_FLAG) ?: return
        }
    }

    override fun renderUi() {
    }

    override fun onResume() {
        FirebaseEventUtils.logEventTracking(context, "welcome_view")
        super.onResume()
        adsHomeViewModel.run {
            preloadNativeHome()
        }
    }

    companion object {
        fun newInstance() = WelcomeFragment()
    }
}