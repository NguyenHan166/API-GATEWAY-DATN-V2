package com.asoft.artsal.photo.ui.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.FragmentGalleryPhotoBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.component.GridSpacingItemDecoration
import com.asoft.artsal.photo.data.model.GalleryPhoto
import com.asoft.artsal.photo.extensions.collectIn
import com.asoft.artsal.photo.extensions.dpToPx
import com.asoft.artsal.photo.extensions.internal.getAdsKey
import com.asoft.artsal.photo.extensions.invisible
import com.asoft.artsal.photo.extensions.navigateToWithAnim
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.popBackStack
import com.asoft.artsal.photo.extensions.visible
import com.asoft.artsal.photo.ui.ads.AdsHomeViewModel
import com.asoft.artsal.photo.utils.AdsIdUtils
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import com.asoft.artsal.photo.utils.RemoteConfigAdsConst
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.minsap.ad.listener.BannerAdCallBack
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GalleryPhotoFragment : BaseFragment<FragmentGalleryPhotoBinding>() {
    @Inject
    lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    private val galleryViewModel by activityViewModels<GalleryPhotoViewModel>()
    private val adsHomeViewModel by activityViewModels<AdsHomeViewModel>()
    private val galleryAdapter by lazy { GalleryPhotoAdapter(::onItemClick) }
    private val selectedPhotosAdapter by lazy { SelectedPhotosAdapter(::onSelectedItemRemove) }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            galleryViewModel.loadImages()
        } else {

        }
    }

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentGalleryPhotoBinding = FragmentGalleryPhotoBinding.inflate(inflater, container, false)

    override fun initListener() {

        setFragmentResultListener(
            REQUEST_PHOTO_LIMITED_KEY,
        ) { _, bundle ->
            val limit = bundle.getInt(RESULT_PHOTO_LIMITED_KEY)
            val type = bundle.getString(RESULT_TYPE)
            galleryViewModel.galleryType = type
            galleryViewModel.setPhotoLimit(limit)
        }

        binding?.btnBack?.onClick {
            FirebaseEventUtils.logEventTracking(context, "select_img_back")

            popBackStack()
        }

        binding?.viewPhotosSelected?.btnNext?.onClick {
            FirebaseEventUtils.logEventTracking(context, "select_img_next")

            // Setup Data
            val selectedPhotos = galleryViewModel.imagesSelected.value
            val gson = Gson()

            val jsonString = gson.toJson(selectedPhotos?.map { it.uri.toString() })
            setFragmentResult(
                REQUEST_PHOTOS_KEY,
                bundleOf(RESULT_PHOTOS_KEY to jsonString)
            )

            when (galleryViewModel.galleryType) {
                TEMPLATE -> {
                    popBackStack()
                }

                COLLAGE -> {
                    navigateToWithAnim(R.id.action_gallery_photo_fragment_to_editor_collage_fragment)
                }
            }
        }
    }

    override fun setupUi() {
        binding?.rvPhotos?.run {
            layoutManager = GridLayoutManager(context ?: return, 3)
            addItemDecoration(GridSpacingItemDecoration(3, 4.dpToPx(), 4.dpToPx(), false, 0))
            adapter = galleryAdapter
        }

        binding?.viewPhotosSelected?.rvImageSelected?.run {
            layoutManager =
                LinearLayoutManager(context ?: return, LinearLayoutManager.HORIZONTAL, false)
            adapter = selectedPhotosAdapter
        }
        binding?.viewPhotosSelected?.root?.invisible()

        checkAndRequestPermission()
    }

    override fun renderUi() {
        loadBannerAds()

        galleryViewModel.run {
            listImages.collectIn(this@GalleryPhotoFragment) { items ->
                galleryAdapter.submitList(items)
            }
            imagesSelected.collectIn(this@GalleryPhotoFragment) { selectedPhotos ->
                updateSelectedPhotosView(selectedPhotos)
            }
        }
    }

    private fun onItemClick(gallery: GalleryPhoto, isSelected: Boolean) {
        FirebaseEventUtils.logEventTracking(context, "select_img_add")

        val newSelectedState = if (gallery.isSelected) false else isSelected
        galleryViewModel.selectImage(gallery.copy(isSelected = newSelectedState))
    }

    private fun onSelectedItemRemove(gallery: GalleryPhoto) {
        galleryViewModel.selectImage(gallery.copy(isSelected = false))
    }

    private fun updateSelectedPhotosView(selectedPhotos: List<GalleryPhoto>?) {
        binding?.viewPhotosSelected?.run {
            selectedPhotos?.let { photos ->
                if (photos.isNotEmpty()) {
                    selectedPhotosAdapter.submitList(photos)

                    root.visible()
                    root.bringToFront()
                    tvAppName.text =
                        getString(
                            R.string.selected_images,
                            photos.size,
                            galleryViewModel.photoLimited
                        )

                    if (photos.size == galleryViewModel.photoLimited) {
                        btnNext.apply {
                            alpha = 1.0f
                            isEnabled = true
                        }
                    } else {
                        btnNext.apply {
                            alpha = 0.5f
                            isEnabled = false
                        }
                    }

                } else {
                    root.invisible()
                }
            } ?: run {
                root.invisible()
            }
        }
    }

    private fun checkAndRequestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                galleryViewModel.loadImages()
            }

            else -> {
                permissionLauncher.launch(permission)
            }
        }
    }

    override fun handleBackPress() {
        FirebaseEventUtils.logEventTracking(context, "select_img_back")

        popBackStack()
        super.handleBackPress()
    }

    private fun loadBannerAds() {
        if (firebaseRemoteConfig.getAdsKey(
                context,
                RemoteConfigAdsConst.BANNER_SELECT_PHOTO_SHOW
            )
        ) {
            binding?.frameBanner?.root?.visible()
            adsHomeViewModel.createBannerGalleryPhoto.loadAndShowBannerAd(
                activity,
                AdsIdUtils.BANNER_SELECT_PHOTO_SHOW,
                adName = "Gallery",
                frameAd = binding?.frameBanner?.root,
                callBackResult = object : BannerAdCallBack() {
                })
        }
    }

    override fun onDestroyView() {
        galleryViewModel.clearData()
        super.onDestroyView()
    }

    override fun onDestroy() {
        adsHomeViewModel.createBannerGalleryPhoto.destroy()
        super.onDestroy()
    }

    companion object {
        const val TEMPLATE = "TEMPLATE"
        const val COLLAGE = "COLLAGE"
        const val RESULT_TYPE = "result_type"
        const val REQUEST_PHOTO_LIMITED_KEY = "request_limited_key"
        const val RESULT_PHOTO_LIMITED_KEY = "result_limited_key"
        const val REQUEST_PHOTOS_KEY = "request_photos_key"
        const val RESULT_PHOTOS_KEY = "result_photos_key"

    }
}