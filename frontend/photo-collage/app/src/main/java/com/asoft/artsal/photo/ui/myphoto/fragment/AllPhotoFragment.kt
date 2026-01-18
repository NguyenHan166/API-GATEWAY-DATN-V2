package com.asoft.artsal.photo.ui.myphoto.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.FragmentAllPhotoBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.component.GridSpacingItemDecoration
import com.asoft.artsal.photo.data.model.Photo
import com.asoft.artsal.photo.data.model.PhotoType
import com.asoft.artsal.photo.extensions.collectIn
import com.asoft.artsal.photo.extensions.dpToPx
import com.asoft.artsal.photo.extensions.gone
import com.asoft.artsal.photo.extensions.initRecyclerViewAdapter
import com.asoft.artsal.photo.extensions.internal.getAdsKey
import com.asoft.artsal.photo.extensions.loadImageDrawableWithCompress
import com.asoft.artsal.photo.extensions.navigateToWithAnim
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.popBackStack
import com.asoft.artsal.photo.extensions.visible
import com.asoft.artsal.photo.ui.ads.AdsHomeViewModel
import com.asoft.artsal.photo.ui.collage.bottomsheet.BackgroundBottomSheetFragment
import com.asoft.artsal.photo.ui.home.viewmodel.EditImageViewModel
import com.asoft.artsal.photo.ui.myphoto.OptionBottomSheetFragment
import com.asoft.artsal.photo.ui.myphoto.adapter.AllPhotoAdapter
import com.asoft.artsal.photo.ui.myphoto.viewmodel.MyPhotoViewModel
import com.asoft.artsal.photo.utils.AdsIdUtils
import com.asoft.artsal.photo.utils.RemoteConfigAdsConst
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.minsap.ad.listener.BannerAdCallBack
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AllPhotoFragment : BaseFragment<FragmentAllPhotoBinding>() {

    @Inject
    lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    private val myPhotoViewModel by activityViewModels<MyPhotoViewModel>()
    private val adsHomeViewModel: AdsHomeViewModel by activityViewModels<AdsHomeViewModel>()

    private val editImageViewModel by activityViewModels<EditImageViewModel>()
    private val allAdapter by lazy {
        AllPhotoAdapter(
            onOptionClick = { photo ->
                onClickOption(photo)
            },
            onPhotoClick = { photo ->
                onClickPhoto(photo)
            }
        )
    }

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAllPhotoBinding {
        return FragmentAllPhotoBinding.inflate(inflater, container, false)
    }

    override fun initListener() {
        binding?.header?.btnBack?.onClick {
            popBackStack()
        }

    }

    override fun setupUi() {
        binding?.rvAllPhotos?.apply {
            initRecyclerViewAdapter(
                yourAdapter = allAdapter,
                yourLayoutManager = GridLayoutManager(requireContext(), 2),
                fixedSize = true,
            )
            addItemDecoration(
                GridSpacingItemDecoration(
                    2, 16.dpToPx(), 8.dpToPx(), false, 0
                )
            )
        }
    }

    override fun renderUi() {
        loadBannerAds()
        binding?.emptyView?.imgEmpty?.loadImageDrawableWithCompress(R.drawable.img_my_photo_empty)
        myPhotoViewModel.allPhotoSelected.collectIn(
            this@AllPhotoFragment,
            action = { data ->
                if (data.first.isEmpty()) {
                    binding?.emptyView?.root?.visible()
                    binding?.rvAllPhotos?.gone()
                } else {
                    binding?.emptyView?.root?.gone()
                    allAdapter.submitList(data.first)
                    binding?.rvAllPhotos?.visible()
                }
            }
        )

        myPhotoViewModel.typePhotoSelected.collectIn(
            this@AllPhotoFragment,
            action = {
                binding?.header?.tvTitleTab?.text = when (it) {
                    PhotoType.Recent -> getString(R.string.recent)
                    PhotoType.Template -> getString(R.string.templates)
                    PhotoType.Collages -> getString(R.string.collages)
                    PhotoType.AllPhoto -> getString(R.string.all_photos)
                    else -> ""
                }
            }
        )
    }

    private fun loadBannerAds() {
        if (firebaseRemoteConfig.getAdsKey(context, RemoteConfigAdsConst.BANNER_MY_PHOTO_SHOW)) {
            binding?.adsBanner?.root?.visible()
            myPhotoViewModel.createBannerAd.loadAndShowBannerAd(
                activity,
                AdsIdUtils.BANNER_MY_PHOTO,
                adName = "My Photo",
                frameAd = binding?.adsBanner?.root,
                callBackResult = object : BannerAdCallBack() {})
        }
    }


    private fun onClickOption(photo: Photo) {
        myPhotoViewModel.setPhotoSelected(photo)
        OptionBottomSheetFragment.newInstance().apply {
            onDeleteItem = {
                myPhotoViewModel.deletePhoto()
            }
        }.show(
            childFragmentManager,
            BackgroundBottomSheetFragment::class.java.simpleName
        )
    }

    private fun onClickPhoto(photo: Photo) {
        adsHomeViewModel.preloadNativeAdSaveImage()
        editImageViewModel.setResultBitmap(photo.uri)
        navigateToWithAnim(R.id.action_all_photo_fragment_to_save_result_fragment)
    }

    override fun onDestroyView() {
        myPhotoViewModel.createBannerAd.destroy()
        super.onDestroyView()
    }

    companion object {

    }
}

