package com.asoft.artsal.photo.ui.collage.bottomsheet

import android.content.DialogInterface
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.BottomSheetTextEditorBinding
import com.artsal.photo.editor.collage.maker.databinding.ItemTabAddTextBinding
import com.artsal.photo.editor.collage.maker.databinding.TestFragmentBinding
import com.asoft.artsal.photo.base.BaseSafeDialogFragment
import com.asoft.artsal.photo.component.GridSpacingItemDecoration
import com.asoft.artsal.photo.data.fontTypes
import com.asoft.artsal.photo.data.model.FontType
import com.asoft.artsal.photo.extensions.collectIn
import com.asoft.artsal.photo.extensions.dpToPx
import com.asoft.artsal.photo.extensions.focusAndShowKeyboard
import com.asoft.artsal.photo.extensions.gone
import com.asoft.artsal.photo.extensions.hideKeyboard
import com.asoft.artsal.photo.extensions.initRecyclerViewAdapter
import com.asoft.artsal.photo.extensions.internal.getAdsKey
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.showKeyboard
import com.asoft.artsal.photo.extensions.visible
import com.asoft.artsal.photo.ui.ads.AdsHomeViewModel
import com.asoft.artsal.photo.ui.collage.adapter.text.ColorTypeAdapter
import com.asoft.artsal.photo.ui.collage.adapter.text.FontStyleAdapter
import com.asoft.artsal.photo.ui.collage.viewmodel.CollageViewModel
import com.asoft.artsal.photo.utils.AdsIdUtils
import com.asoft.artsal.photo.utils.AppUtils
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import com.asoft.artsal.photo.utils.RemoteConfigAdsConst
import com.google.android.material.tabs.TabLayout
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.minsap.ad.listener.BannerAdCallBack
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TestFragment : BaseSafeDialogFragment<TestFragmentBinding>() {

    private val collageViewModel: CollageViewModel by activityViewModels<CollageViewModel>()
    private val adsHomeViewModel: AdsHomeViewModel by activityViewModels<AdsHomeViewModel>()

    @Inject
    lateinit var firebaseRemoteConfig: FirebaseRemoteConfig


    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): TestFragmentBinding {
        return TestFragmentBinding.inflate(layoutInflater, container, false)
    }


    override fun getTheme(): Int {
        return R.style.KeyboardInputStyle
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun setupUi() {
        binding?.editText?.focusAndShowKeyboard()
    }

    companion object {
        @JvmStatic
        fun newInstance() = TestFragment()
    }
}
