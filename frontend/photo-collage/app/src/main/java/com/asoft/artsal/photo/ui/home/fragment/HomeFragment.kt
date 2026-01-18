package com.asoft.artsal.photo.ui.home.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.FragmentHomeBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.component.GridSpacingItemDecoration
import com.asoft.artsal.photo.data.model.Photo
import com.asoft.artsal.photo.extensions.collectIn
import com.asoft.artsal.photo.extensions.dpToPx
import com.asoft.artsal.photo.extensions.getAdNativeMediumSquareHomeBinding
import com.asoft.artsal.photo.extensions.gone
import com.asoft.artsal.photo.extensions.initRecyclerViewAdapter
import com.asoft.artsal.photo.extensions.internal.getAdsKey
import com.asoft.artsal.photo.extensions.loadImageDrawableWithCompress
import com.asoft.artsal.photo.extensions.navigateToWithAnim
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.toast
import com.asoft.artsal.photo.extensions.visible
import com.asoft.artsal.photo.ui.ads.AdsHomeViewModel
import com.asoft.artsal.photo.ui.home.adapter.PhotoHomeAdapter
import com.asoft.artsal.photo.ui.home.viewmodel.EditImageViewModel
import com.asoft.artsal.photo.ui.home.viewmodel.HomeViewModel
import com.asoft.artsal.photo.ui.myphoto.OptionBottomSheetFragment
import com.asoft.artsal.photo.ui.myphoto.viewmodel.MyPhotoViewModel
import com.asoft.artsal.photo.ui.settings.fragment.RatingDialog
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import com.asoft.artsal.photo.utils.PermissionManager
import com.asoft.artsal.photo.utils.RemoteConfigAdsConst
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.minsap.ad.ads.common.NativeAdState
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    override val isInsets: Boolean
        get() = false

    override val isLightStatusBar: Boolean
        get() = false

    @Inject
    lateinit var firebaseRemoteConfig: FirebaseRemoteConfig

    private val permissionManager: PermissionManager = PermissionManager()
    private val adsHomeViewModel: AdsHomeViewModel by activityViewModels<AdsHomeViewModel>()
    private val myPhotoViewModel: MyPhotoViewModel by activityViewModels<MyPhotoViewModel>()
    private val editImageViewModel: EditImageViewModel by activityViewModels<EditImageViewModel>()
    private val homeViewModel: HomeViewModel by activityViewModels<HomeViewModel>()


    private val recentAdapter by lazy {
        PhotoHomeAdapter(
            onOptionClick = { photo ->
                onOptionClick(photo)
            },
            onPhotoClick = { photo ->
                onPhotoClick(photo)
            }
        )
    }

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionManager.initializePermissionLauncher(
            requireContext(),
            activity ?: return,
            fragment = this
        )
    }


    override fun initListener() {
        binding?.run {
            btnCreateCollage.onClick {
                navigateToWithAnim(R.id.action_home_fragment_to_collage_fragment)
            }
            btnTemplate.onClick {
                navigateToWithAnim(R.id.action_home_fragment_to_template_fragment)

            }
            btnBlankCanvas.onClick {
                navigateToWithAnim(R.id.action_home_fragment_to_blank_canvas_fragment)

            }
            tvAllRecent.onClick {
                navigateToWithAnim(R.id.action_home_fragment_to_my_photo_fragment)

            }

            appBar.btnSetting.onClick {
                navigateToWithAnim(R.id.action_home_fragment_to_settings_fragment)
            }
        }
    }

    override fun setupUi() {
        binding?.run {
            imgBg.loadImageDrawableWithCompress(R.drawable.img_bg_home)
        }

        binding?.rvRecent?.apply {
            initRecyclerViewAdapter(
                yourAdapter = recentAdapter,
                yourLayoutManager = GridLayoutManager(requireContext(), 2),
                fixedSize = false,
            )
            addItemDecoration(
                GridSpacingItemDecoration(
                    2, 12.dpToPx(), 12.dpToPx(), false, 0
                )
            )
        }
//        homeViewModel.initDataAsset()
    }

    override fun renderUi() {
        Timber.i("Loanding Ads...")
        adsHomeViewModel.preloadNativeHome()
        if (firebaseRemoteConfig.getAdsKey(context, RemoteConfigAdsConst.NATIVE_HOME_SHOW)) {
            binding?.frameNative?.root?.visible()
            adsHomeViewModel.adNativeHome.collectIn(this) { adNativeState ->
                when (adNativeState) {
                    is NativeAdState.NativeAdData -> {
                        val adsBinding = getAdNativeMediumSquareHomeBinding()
                        adsHomeViewModel.createAdNativeHome.showNativeAd(
                            frameAd = binding?.frameNative?.root,
                            adsBinding = adsBinding,
                            nativeAdValue = adNativeState.nativeAdValue,
                            subscribe = adsHomeViewModel.getAdNativeHome(),
                            isConfigRatio = true,
                            isReload = true
                        )
                    }

                    is NativeAdState.NativeAdError -> {
                        binding?.frameNative?.root?.gone()
                    }

                    else -> {}
                }
            }
        }

        Timber.i("Check Permissions...")
        if (permissionManager.hasImagePermissions(requireContext())) {
            myPhotoViewModel.loadImages()
        } else {
            permissionManager.requestPermissions(
                permissions = permissionManager.getRequireImagePermissions(),
                title = getString(R.string.permission_title),
                txtRationale = getString(R.string.rationale_access_photo),
                txtPermanentlyDenied = getString(R.string.enable_permission_access_photo),
            ) { isGranted ->
                if (isGranted) {
                    myPhotoViewModel.loadImages()
                } else {
                    toast(getString(R.string.permission_denied_cannot_load_images))
                }
            }
        }

        Timber.i("Setup collect data images...")
        myPhotoViewModel.recentPhotos.collectIn(
            this@HomeFragment,
            action = {
                myPhotoViewModel.setUiRecentData(isShowRecent = it.isNotEmpty())
                recentAdapter.submitList(it)
            }
        )

        Timber.i("Setup collect visible recent...")
        myPhotoViewModel.dataUi.collectIn(
            this@HomeFragment,
            minActiveState = Lifecycle.State.RESUMED,
            action = {
                if (it.isShowRecent) binding?.containerRecent?.visible() else binding?.containerRecent?.gone()
            }
        )
    }

    override fun onResume() {
        super.onResume()
        if (homeViewModel.shouldShowReviewDialog()) {
            RatingDialog.newInstance()
                .show(childFragmentManager, RatingDialog::class.java.simpleName)
            homeViewModel.saveTimeShowDialog()
        }
    }
    private fun onOptionClick(photo: Photo) {
        FirebaseEventUtils.logEventTracking(context, "myphoto_click_item")

        myPhotoViewModel.setPhotoSelected(photo)
        OptionBottomSheetFragment.newInstance().apply {
        }.show(
            childFragmentManager,
            OptionBottomSheetFragment::class.java.simpleName
        )
    }

    private fun onPhotoClick(photo: Photo) {
        adsHomeViewModel.preloadNativeAdSaveImage()
        editImageViewModel.setResultBitmap(photo.uri)
        navigateToWithAnim(R.id.action_home_fragment_to_save_result_fragment)
    }

    override fun onDestroyView() {
        adsHomeViewModel.createAdNativeHome.destroy()
        super.onDestroyView()
    }

}