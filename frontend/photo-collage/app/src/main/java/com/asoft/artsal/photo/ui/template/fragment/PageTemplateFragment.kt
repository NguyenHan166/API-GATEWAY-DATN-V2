package com.asoft.artsal.photo.ui.template.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.AdsNativeMediumSquareBinding
import com.artsal.photo.editor.collage.maker.databinding.FragmentPageTemplateBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.component.ForumStaggeredDecoration
import com.asoft.artsal.photo.data.model.Template
import com.asoft.artsal.photo.data.model.TemplateType
import com.asoft.artsal.photo.extensions.collectIn
import com.asoft.artsal.photo.extensions.collectNativeAdData
import com.asoft.artsal.photo.extensions.dpToPx
import com.asoft.artsal.photo.extensions.findViewHolderByViewType
import com.asoft.artsal.photo.extensions.getAdNativeMediumSquareBinding
import com.asoft.artsal.photo.extensions.initRecyclerViewAdapter
import com.asoft.artsal.photo.extensions.parentNavigateTo
import com.asoft.artsal.photo.extensions.serializable
import com.asoft.artsal.photo.ui.home.viewmodel.HomeViewModel
import com.asoft.artsal.photo.ui.template.adapter.SelectTemplateAdapter
import com.asoft.artsal.photo.ui.template.adapter.SelectTemplateView
import com.asoft.artsal.photo.ui.template.adapter.TemplateTypeTabs
import com.asoft.artsal.photo.ui.template.viewmodel.TemplateViewModel
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import com.minsap.ad.ads.common.NativeAdState
import timber.log.Timber

class PageTemplateFragment : BaseFragment<FragmentPageTemplateBinding>() {
    override val isInsets: Boolean
        get() = false
    override val isLightStatusBar: Boolean
        get() = false
    private var adsNativeSquareBinding: AdsNativeMediumSquareBinding? = null
    private val homeViewModel: HomeViewModel by activityViewModels<HomeViewModel>()
    private val templateViewModel: TemplateViewModel by activityViewModels<TemplateViewModel>()
    private var handlerBindView: Handler? = null

    private val selectTemplateAdapter by lazy {
        SelectTemplateAdapter(
            ::onSelect
        )
    }

    private var templateType: TemplateTypeTabs? = null

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPageTemplateBinding {
        return FragmentPageTemplateBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        templateType = arguments?.serializable(TYPE_TEMPLATE)
        Timber.i("onCReate ===> templateType: $templateType")

    }

    override fun initListener() {

    }

    override fun setupUi() {
        val staggeredGridLayoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding?.rclFrameTemplate?.addItemDecoration(
            ForumStaggeredDecoration(requireContext(), 8.dpToPx())
        )
        binding?.rclFrameTemplate?.initRecyclerViewAdapter(
            yourAdapter = selectTemplateAdapter,
            yourLayoutManager = staggeredGridLayoutManager,
            fixedSize = true,
        )
        val tab = getTemplateType()
        Timber.i("tab setup: $tab")
        homeViewModel.setupDataTemplateByType(
            tab ?: TemplateType.Trending
        )
    }

    private fun getTemplateType(): TemplateType? {
        return when (templateType) {
            TemplateTypeTabs.All -> TemplateType.All
            TemplateTypeTabs.Trending -> TemplateType.Trending
            TemplateTypeTabs.Mutable -> homeViewModel.getMutableTypeSelected()
            TemplateTypeTabs.SpecialDays -> TemplateType.SpecialDays
            else -> TemplateType.All
        }
    }

    override fun renderUi() {
        when (templateType) {
            TemplateTypeTabs.All -> {
                Timber.i("renderUi: ${templateType}")
                homeViewModel.selectTempViewAll.collectIn(
                    this, Lifecycle.State.RESUMED
                ) { selectViews -> addDataInView(selectViews) }
            }

            TemplateTypeTabs.Trending -> {
                Timber.i("renderUi: ${templateType}")
                homeViewModel.selectTempTrend.collectIn(
                    this, Lifecycle.State.RESUMED
                ) { selectViews -> addDataInView(selectViews) }
            }

            TemplateTypeTabs.Mutable -> {
                Timber.i("renderUi: ${templateType}")
                homeViewModel.selectView.collectIn(
                    this, Lifecycle.State.RESUMED
                ) { selectViews -> addDataInView(selectViews) }

            }

            TemplateTypeTabs.SpecialDays -> {
                Timber.i("renderUi: ${templateType}")
                homeViewModel.selectTempViewSpec.collectIn(
                    this, Lifecycle.State.RESUMED
                ) { selectViews ->
                    addDataInView(selectViews)
                    Timber.i("selectViews: $selectViews")
                }
            }

            else -> {}
        }
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.preloadNativeAdTempHome()
    }

    private fun addDataInView(selectViews: List<SelectTemplateView>?) {
        if (selectViews?.isNotEmpty() ?: return) {
            Timber.i("$selectViews.size")
            selectTemplateAdapter.submitList(selectViews)
            handlerBindView = Handler(Looper.getMainLooper())
            handlerBindView?.postDelayed({
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    val holderAds = binding?.rclFrameTemplate?.findViewHolderByViewType(
                        SelectTemplateAdapter.TYPE_ADS
                    ) as? SelectTemplateAdapter.NativeAdsViewHolder
                    homeViewModel.adNativeTempHome.collectNativeAdData(this)
                    { adNativeState ->
                        when (adNativeState) {
                            is NativeAdState.NativeAdData -> {
                                adsNativeSquareBinding = null
                                adsNativeSquareBinding = getAdNativeMediumSquareBinding()
                                holderAds?.bind(
                                    createAdNative = homeViewModel.createAdNativeTempHome,
                                    adsBinding = adsNativeSquareBinding,
                                    nativeAdValue = adNativeState.nativeAdValue,
                                    subscribe = homeViewModel.getAdNativeTempHome(),
                                )
                            }

                            is NativeAdState.NativeAdError -> {
                                Timber.e("NativeAdError: ${adNativeState.adError}")
                                holderAds?.bindError()
                            }

                            else -> {}
                        }
                    }
                }
            }, 100)
        }
    }

    private fun onSelect(template: Template) {
        FirebaseEventUtils.logEventTracking(context, "home_select_template")
        templateViewModel.updateTemplate(template)
        parentNavigateTo(R.id.action_template_fragment_to_editor_template_fragment)
    }

    override fun onDestroyView() {
        handlerBindView?.removeCallbacksAndMessages(null)
        handlerBindView = null
        adsNativeSquareBinding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        homeViewModel.createAdNativeTempHome.destroy()
    }

    companion object {

        private const val TYPE_TEMPLATE = "TYPE_TEMPLATE"

        fun newInstance(templateType: TemplateTypeTabs) = PageTemplateFragment().apply {
            Timber.i(" newInstance with templateType$templateType")
            arguments = Bundle(1).apply {
                putSerializable(TYPE_TEMPLATE, templateType)
            }
        }
    }
}