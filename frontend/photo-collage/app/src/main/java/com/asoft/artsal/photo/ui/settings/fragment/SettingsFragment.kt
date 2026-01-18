package com.asoft.artsal.photo.ui.settings.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.FragmentSettingsBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.extensions.invisible
import com.asoft.artsal.photo.extensions.navigateTo
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.openBrowser
import com.asoft.artsal.photo.extensions.openChPlay
import com.asoft.artsal.photo.extensions.popBackStack
import com.asoft.artsal.photo.extensions.sendFeedback
import com.asoft.artsal.photo.extensions.shareLink
import com.asoft.artsal.photo.ui.onboard.viewmodel.OnboardingViewModel
import com.asoft.artsal.photo.utils.Const
import com.ironsource.bi
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import kotlin.getValue

@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {

    private val onboardingViewModel by viewModels<OnboardingViewModel>()

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSettingsBinding = FragmentSettingsBinding.inflate(inflater, container, false)

    override fun initListener() {
        binding?.run {
            vToolbar.btnBack.onClick {
                popBackStack()
            }

            llLanguage.onClick {
                navigateTo(id = R.id.action_setting_to_select_language)
            }

            tvShareApp.onClick {
                activity?.shareLink(link = Const.LINK_SHARE_APP)
            }

            tvFeedback.onClick {
                activity?.sendFeedback("feedback@qtonzglobal.com", "Photo Collage: UserFeedback")
            }

            tvRateUs.onClick {
                activity?.openChPlay(fullLink = Const.LINK_STORE)
            }

            tvPrivacy.onClick {
                context?.openBrowser(url = Const.LINK_PRIVACY)
            }
        }
    }

    override fun setupUi() {
        binding?.vToolbar?.apply {
            tvAppName.text = getString(R.string.settings)
            btnSave.invisible()
        }
    }

    override fun renderUi() {
        onboardingViewModel.languages.observe(viewLifecycleOwner) { list ->
            val checked = list.firstOrNull { it.isChecked } ?: list.firstOrNull()
            binding?.tvValue?.text = checked?.name ?: binding?.tvValue?.text
        }
        
        onboardingViewModel.getLanguagesSetting()
    }
}