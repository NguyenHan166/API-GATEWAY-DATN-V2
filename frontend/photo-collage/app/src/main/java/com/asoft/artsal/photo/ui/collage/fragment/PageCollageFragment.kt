package com.asoft.artsal.photo.ui.collage.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.AdsNativeMediumSquareBinding
import com.artsal.photo.editor.collage.maker.databinding.FragmentPageCollageBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.component.ForumStaggeredDecoration
import com.asoft.artsal.photo.data.model.Collage
import com.asoft.artsal.photo.extensions.collectIn
import com.asoft.artsal.photo.extensions.collectNativeAdData
import com.asoft.artsal.photo.extensions.dpToPx
import com.asoft.artsal.photo.extensions.findViewHolderByViewType
import com.asoft.artsal.photo.extensions.getAdNativeMediumSquareBinding
import com.asoft.artsal.photo.extensions.initRecyclerViewAdapter
import com.asoft.artsal.photo.extensions.parentNavigateTo
import com.asoft.artsal.photo.extensions.serializable
import com.asoft.artsal.photo.ui.collage.adapter.CollageType
import com.asoft.artsal.photo.ui.collage.adapter.SelectCollageAdapter
import com.asoft.artsal.photo.ui.collage.adapter.SelectView
import com.asoft.artsal.photo.ui.collage.viewmodel.CollageViewModel
import com.asoft.artsal.photo.ui.gallery.GalleryPhotoFragment.Companion.COLLAGE
import com.asoft.artsal.photo.ui.gallery.GalleryPhotoFragment.Companion.REQUEST_PHOTO_LIMITED_KEY
import com.asoft.artsal.photo.ui.gallery.GalleryPhotoFragment.Companion.RESULT_PHOTO_LIMITED_KEY
import com.asoft.artsal.photo.ui.gallery.GalleryPhotoFragment.Companion.RESULT_TYPE
import com.asoft.artsal.photo.ui.home.viewmodel.HomeViewModel
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import com.minsap.ad.ads.common.NativeAdState
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class PageCollageFragment : BaseFragment<FragmentPageCollageBinding>() {
    override val isInsets: Boolean
        get() = false
    override val isLightStatusBar: Boolean
        get() = false
    private var adsNativeSquareBinding: AdsNativeMediumSquareBinding? = null
    private val homeViewModel: HomeViewModel by activityViewModels<HomeViewModel>()
    private var collageType: CollageType? = null
    private var handlerBindView: Handler? = null

    private var pickerNumbers: Int = 5

    private val selectCollageAdapter by lazy {
        SelectCollageAdapter(
            ::onSelect
        )
    }

    private val collageViewModel: CollageViewModel by activityViewModels<CollageViewModel>()

    override fun onInflateView(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentPageCollageBinding {
        return FragmentPageCollageBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collageType = arguments?.serializable(TYPE_COLLAGE)
        pickerNumbers = when (collageType) {
            CollageType.Collage2 -> 2
            CollageType.Collage3 -> 3
            CollageType.Collage4 -> 4
            else -> 5
        }
    }

    override fun initListener() {

    }

    override fun setupUi() {
        val staggeredGridLayoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding?.recyclerView?.addItemDecoration(
            ForumStaggeredDecoration(requireContext(), 8.dpToPx())
        )
        binding?.recyclerView?.initRecyclerViewAdapter(
            yourAdapter = selectCollageAdapter,
            yourLayoutManager = staggeredGridLayoutManager,
            fixedSize = true,
        )
        homeViewModel.setupDataByType(collageType ?: CollageType.All)
    }

    override fun renderUi() {
        when (collageType) {
            CollageType.All -> {
                homeViewModel.selectViewAll.collectIn(
                    this, Lifecycle.State.RESUMED
                ) { selectViews -> addDataInView(selectViews) }
            }

            CollageType.Collage2 -> {
                homeViewModel.selectView2.collectIn(
                    this, Lifecycle.State.RESUMED
                ) { selectViews -> addDataInView(selectViews) }
            }

            CollageType.Collage3 -> {
                homeViewModel.selectView3.collectIn(
                    this, Lifecycle.State.RESUMED
                ) { selectViews -> addDataInView(selectViews) }

            }

            CollageType.Collage4 -> {
                homeViewModel.selectView4.collectIn(
                    this, Lifecycle.State.RESUMED
                ) { selectViews -> addDataInView(selectViews) }
            }

            CollageType.Collage5 -> {
                homeViewModel.selectView5.collectIn(
                    this, Lifecycle.State.RESUMED
                ) { selectViews -> addDataInView(selectViews) }
            }

            else -> {}
        }
    }

    private fun onSelect(collage: Collage) {
        FirebaseEventUtils.logEventTracking(context, "collage_select")
        navToGallery(collage.photoNumbers)
        collageViewModel.updateDataCollage(collage)
    }

    private fun navToGallery(photoNumbers: Int) {
        requireParentFragment().setFragmentResult(
            REQUEST_PHOTO_LIMITED_KEY,
            bundleOf(
                RESULT_TYPE to COLLAGE,
                RESULT_PHOTO_LIMITED_KEY to photoNumbers
            )
        )
        parentNavigateTo(R.id.action_collage_fragment_to_gallery_photo_fragment)
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.preloadNativeAdCollage()
    }

    private fun addDataInView(selectViews: List<SelectView>?) {
        if (selectViews?.isNotEmpty() ?: return) {
            Timber.i("$selectViews.size")
            selectCollageAdapter.submitList(selectViews)
            handlerBindView = Handler(Looper.getMainLooper())
            handlerBindView?.postDelayed({
                adsNativeSquareBinding = null
                adsNativeSquareBinding = AdsNativeMediumSquareBinding.inflate(layoutInflater)
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    val holderAds = binding?.recyclerView?.findViewHolderByViewType(
                        SelectCollageAdapter.TYPE_ADS
                    ) as? SelectCollageAdapter.NativeAdsViewHolder
                    homeViewModel.adNativeCollage.collectNativeAdData(this)
                    { adNativeState ->
                        when (adNativeState) {
                            is NativeAdState.NativeAdData -> {
                                adsNativeSquareBinding = null
                                adsNativeSquareBinding = getAdNativeMediumSquareBinding()
                                holderAds?.bind(
                                    createAdNative = homeViewModel.createAdNativeCollage,
                                    adsBinding = adsNativeSquareBinding,
                                    nativeAdValue = adNativeState.nativeAdValue,
                                    subscribe = homeViewModel.getAdNativeCollage(),
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

    override fun onDestroyView() {
        handlerBindView?.removeCallbacksAndMessages(null)
        handlerBindView = null
        adsNativeSquareBinding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        homeViewModel.createAdNativeCollage.destroy()
    }

    companion object {
        private const val TYPE_COLLAGE = "TYPE_COLLAGE"

        fun newInstance(collageType: CollageType) = PageCollageFragment().apply {
            arguments = Bundle(1).apply {
                putSerializable(TYPE_COLLAGE, collageType)
            }
        }
    }
}