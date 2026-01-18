package com.asoft.artsal.photo.ui.myphoto.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.FragmentMyPhotoBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.data.model.Photo
import com.asoft.artsal.photo.data.model.PhotoType
import com.asoft.artsal.photo.extensions.collectIn
import com.asoft.artsal.photo.extensions.gone
import com.asoft.artsal.photo.extensions.initRecyclerViewAdapter
import com.asoft.artsal.photo.extensions.loadImageDrawableWithCompress
import com.asoft.artsal.photo.extensions.navigateToWithAnim
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.parentNavigateTo
import com.asoft.artsal.photo.extensions.popBackStack
import com.asoft.artsal.photo.extensions.toast
import com.asoft.artsal.photo.extensions.visible
import com.asoft.artsal.photo.ui.ads.AdsHomeViewModel
import com.asoft.artsal.photo.ui.home.viewmodel.EditImageViewModel
import com.asoft.artsal.photo.ui.home.viewmodel.HomeViewModel
import com.asoft.artsal.photo.ui.myphoto.OptionBottomSheetFragment
import com.asoft.artsal.photo.ui.myphoto.adapter.MyPhotoAdapter
import com.asoft.artsal.photo.ui.myphoto.viewmodel.MyPhotoViewModel
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import com.asoft.artsal.photo.utils.PermissionManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MyPhotoFragment : BaseFragment<FragmentMyPhotoBinding>() {
    override val isInsets: Boolean
        get() = true
    private val myPhotoViewModel by activityViewModels<MyPhotoViewModel>()

    private val editImageViewModel: EditImageViewModel by activityViewModels<EditImageViewModel>()

    private val adsHomeViewModel: AdsHomeViewModel by activityViewModels<AdsHomeViewModel>()

    private val recentAdapter by lazy {
        MyPhotoAdapter(
            onOptionClick = { photo -> onOptionClick(photo) },
            onPhotoClick = { photo -> onPhotoClick(photo) }
        )
    }

    private val templateAdapter by lazy {
        MyPhotoAdapter(
            onOptionClick = { photo -> onOptionClick(photo) },
            onPhotoClick = { photo -> onPhotoClick(photo) }
        )
    }
    private val collageAdapter by lazy {
        MyPhotoAdapter(
            onOptionClick = { photo -> onOptionClick(photo) },
            onPhotoClick = { photo -> onPhotoClick(photo) }
        )
    }
    private val allPhotoAdapter by lazy {
        MyPhotoAdapter(
            onOptionClick = { photo -> onOptionClick(photo) },
            onPhotoClick = { photo -> onPhotoClick(photo) }
        )
    }

    private val permissionManager: PermissionManager = PermissionManager()

    private val homeViewModel: HomeViewModel by activityViewModels<HomeViewModel>()

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMyPhotoBinding = FragmentMyPhotoBinding.inflate(inflater, container, false)

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
            header.btnBack.onClick {
                popBackStack()
            }
            llRecent.onClick {
                myPhotoViewModel.setAllPhotoSelected(
                    myPhotoViewModel.recentPhotos.value,
                    PhotoType.Recent
                )
                onDirectScreen(photoType = PhotoType.Recent)
            }

            llTemplate.onClick {
                myPhotoViewModel.setAllPhotoSelected(
                    myPhotoViewModel.templatePhotos.value,
                    PhotoType.Template
                )
                onDirectScreen(photoType = PhotoType.Template)
            }

            llCollage.onClick {
                myPhotoViewModel.setAllPhotoSelected(
                    myPhotoViewModel.collagePhotos.value,
                    PhotoType.Collages
                )
                onDirectScreen(photoType = PhotoType.Collages)
            }
            llAllPhotos.onClick {
                myPhotoViewModel.setAllPhotoSelected(
                    myPhotoViewModel.allPhotos.value,
                    PhotoType.AllPhoto
                )
                onDirectScreen(photoType = PhotoType.AllPhoto)
            }
        }
    }

    override fun setupUi() {
        FirebaseEventUtils.logEventTracking(context, "myphoto_view")
        binding?.run {
            header.tvTitleTab.setText(R.string.nav_my_photo)
        }
        editImageViewModel.enableSaveImage = false
        binding?.rvRecent?.initRecyclerViewAdapter(
            yourAdapter = recentAdapter,
            yourLayoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            ),
            fixedSize = true
        )

        binding?.rvTemplate?.initRecyclerViewAdapter(
            yourAdapter = templateAdapter,
            yourLayoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            ),
            fixedSize = true
        )

        binding?.rvCollage?.initRecyclerViewAdapter(
            yourAdapter = collageAdapter,
            yourLayoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            ),
            fixedSize = true
        )

        binding?.rvAllPhotos?.initRecyclerViewAdapter(
            yourAdapter = allPhotoAdapter,
            yourLayoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            ),
            fixedSize = true
        )
    }

    override fun renderUi() {
        binding?.emptyView?.apply {
            imgEmpty.loadImageDrawableWithCompress(drawableRes = R.drawable.img_my_photo_empty)
        }
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

        myPhotoViewModel.dataUi.collectIn(
            this@MyPhotoFragment,
            action = {
                if (it.isShowRecent) binding?.containerRecent?.visible() else binding?.containerRecent?.gone()
                if (it.isShowTemplate) binding?.containerTemplate?.visible() else binding?.containerTemplate?.gone()
                if (it.isShowCollage) binding?.containerCollage?.visible() else binding?.containerCollage?.gone()
                if (it.isShowAllPhoto) binding?.containerAllPhoto?.visible() else binding?.containerAllPhoto?.gone()
                if (it.isShowEmptyView) binding?.emptyView?.rootView?.visible() else binding?.emptyView?.rootView?.gone()
            }
        )
        myPhotoViewModel.recentPhotos.collectIn(
            this@MyPhotoFragment,
            action = {
                myPhotoViewModel.setUiRecentData(isShowRecent = it.isNotEmpty())
                recentAdapter.submitList(it)
            }
        )
        myPhotoViewModel.templatePhotos.collectIn(
            this@MyPhotoFragment,
            action = {
                myPhotoViewModel.setUiTemplateData(isShowTemplate = it.isNotEmpty())
                templateAdapter.submitList(it)
            }
        )
        myPhotoViewModel.collagePhotos.collectIn(
            this@MyPhotoFragment,
            action = {
                myPhotoViewModel.setUiCollageData(isShowCollage = it.isNotEmpty())
                collageAdapter.submitList(it)
            }
        )
        myPhotoViewModel.allPhotos.collectIn(
            this@MyPhotoFragment,
            action = {
                myPhotoViewModel.setUiAllPhotoData(isShowAllPhoto = it.isNotEmpty())
                allPhotoAdapter.submitList(it)
            }
        )
    }

    private fun onDirectScreen(photoType: PhotoType) {
        myPhotoViewModel.setTypePhotoSelected(photoType)
        navigateToWithAnim(R.id.action_my_photo_fragment_to_all_photo_fragment)
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
        parentNavigateTo(R.id.action_my_photo_fragment_to_save_result_fragment)
    }
}