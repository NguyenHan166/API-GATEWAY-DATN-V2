package com.asoft.artsal.photo.ui.settings.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.FragmentSelectLanguageBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.component.LinearSpacingItemDecoration
import com.asoft.artsal.photo.extensions.dpToPx
import com.asoft.artsal.photo.extensions.gone
import com.asoft.artsal.photo.extensions.initRecyclerViewAdapter
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.popBackStack
import com.asoft.artsal.photo.extensions.visible
import com.asoft.artsal.photo.ui.onboard.adapter.SelectLanguageAdapter
import com.asoft.artsal.photo.ui.onboard.viewmodel.OnboardingViewModel
import com.asoft.artsal.photo.utils.SharePreference
import com.google.android.gms.internal.ads.zzbtn
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SelectLanguageSettingFragment : BaseFragment<FragmentSelectLanguageBinding>() {

    @Inject
    lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    @Inject
    lateinit var sharePreference: SharePreference

    private val onboardingViewModel by viewModels<OnboardingViewModel>()
    private val selectLanguageAdapter by lazy { SelectLanguageAdapter(::onSelectLanguage) }

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSelectLanguageBinding = FragmentSelectLanguageBinding.inflate(inflater, container, false)

    override fun initListener() {
        binding?.btnSave?.onClick {
            val languageCode = onboardingViewModel.codeLanguage.value
            if (!languageCode.isNullOrEmpty()) {
                sharePreference.save(SharePreference.LANGUAGE_APP, languageCode)
                activity?.recreate()
            }

            popBackStack()
        }

        binding?.btnBack?.onClick {
            popBackStack()
        }
    }

    override fun setupUi() {
        binding?.run {
            tvSelect.gone()
            tvLanguage.visible()
            btnSave.visible()
            btnBack.visible()
            lottieSelect.gone()

            rvLanguages.initRecyclerViewAdapter(
                yourAdapter = selectLanguageAdapter,
                yourLayoutManager = LinearLayoutManager(
                    requireContext(),
                    LinearLayoutManager.VERTICAL,
                    false
                )
            )

            rvLanguages.addItemDecoration(
                LinearSpacingItemDecoration(
                    isHorizontal = false,
                    spacing = 16.dpToPx()
                )
            )
        }
    }

    override fun renderUi() {
        onboardingViewModel.getLanguagesSetting()

        onboardingViewModel.languages.observe(viewLifecycleOwner) {
            selectLanguageAdapter.submitList(it)
        }
    }

    private fun onSelectLanguage(code: String) {
        onboardingViewModel.selectLanguage(code)
    }
}