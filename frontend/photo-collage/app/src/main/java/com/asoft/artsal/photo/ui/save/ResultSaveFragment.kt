package com.asoft.artsal.photo.ui.save

import android.graphics.Bitmap
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.FragmentResultSaveBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.extensions.collectIn
import com.asoft.artsal.photo.extensions.getAdNativeMediumRectangleBinding
import com.asoft.artsal.photo.extensions.internal.getAdsKey
import com.asoft.artsal.photo.extensions.invisible
import com.asoft.artsal.photo.extensions.loadImageDrawable
import com.asoft.artsal.photo.extensions.loadImageDrawableWithCompress
import com.asoft.artsal.photo.extensions.navigateToWithAnim
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.popBackStack
import com.asoft.artsal.photo.extensions.toast
import com.asoft.artsal.photo.extensions.visible
import com.asoft.artsal.photo.ui.ads.AdsHomeViewModel
import com.asoft.artsal.photo.ui.collage.viewmodel.CollageViewModel
import com.asoft.artsal.photo.ui.home.viewmodel.EditImageViewModel
import com.asoft.artsal.photo.ui.template.viewmodel.TemplateViewModel
import com.asoft.artsal.photo.utils.Const
import com.asoft.artsal.photo.utils.Const.APP_FOLDER
import com.asoft.artsal.photo.utils.Const.COLLAGE_FOLDER
import com.asoft.artsal.photo.utils.Const.TEMPLATE_FOLDER
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import com.asoft.artsal.photo.utils.ImageSharingHelper
import com.asoft.artsal.photo.utils.RemoteConfigAdsConst
import com.asoft.artsal.photo.utils.StorageUtils.saveImageToStorage
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.minsap.ad.ads.common.NativeAdState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ResultSaveFragment : BaseFragment<FragmentResultSaveBinding>() {

    @Inject
    lateinit var firebaseRemoteConfig: FirebaseRemoteConfig

    private lateinit var imageSharingHelper: ImageSharingHelper

    private val adsHomeViewModel: AdsHomeViewModel by activityViewModels<AdsHomeViewModel>()
    private val editImageViewModel: EditImageViewModel by activityViewModels<EditImageViewModel>()
    private val templateViewModel: TemplateViewModel by activityViewModels<TemplateViewModel>()
    private val collageViewModel: CollageViewModel by activityViewModels<CollageViewModel>()

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentResultSaveBinding {
        return FragmentResultSaveBinding.inflate(inflater, container, false)
    }

    override fun initListener() {
        binding?.run {
            btnBack.onClick {
                FirebaseEventUtils.logEventTracking(context, "result_click_back")

                popBackStack()
            }
            btnHome.onClick {
                FirebaseEventUtils.logEventTracking(context, "result_click_home")

                if (editImageViewModel.isTemplateFunction) {
                    editImageViewModel.clearDataTemplate()
                    templateViewModel.clearDataTemplate()
                } else {
                    collageViewModel.deleteTempFile()
                    collageViewModel.clearDataTmp()
                }

                navigateToWithAnim(
                    R.id.action_save_result_fragment_to_home_fragment,
                    popUpToId = R.id.save_result_fragment,
                    isInclusive = true
                )
            }
            facebookContainer.onClick {
                FirebaseEventUtils.logEventTracking(context, "result_share")

                imageSharingHelper.shareImageToSpecificApp(
                    editImageViewModel.resultBitmap.value as Any,
                    appPackage = Const.PACKAGE_FACEBOOK
                )
            }
            instagramContainer.onClick {
                FirebaseEventUtils.logEventTracking(context, "result_share")

                imageSharingHelper.shareImageToSpecificApp(
                    editImageViewModel.resultBitmap.value as Any,
                    appPackage = Const.PACKAGE_INSTAGRAM
                )
            }
            messengerContainer.onClick {
                FirebaseEventUtils.logEventTracking(context, "result_share")

                imageSharingHelper.shareImageToSpecificApp(
                    editImageViewModel.resultBitmap.value as Any,
                    appPackage = Const.PACKAGE_MESSENGER
                )
            }
            twitterContainer.onClick {
                FirebaseEventUtils.logEventTracking(context, "result_share")

                imageSharingHelper.shareImageToSpecificApp(
                    editImageViewModel.resultBitmap.value as Any,
                    appPackage = Const.PACKAGE_TWITTER
                )
            }
            moreContainer.onClick {
                FirebaseEventUtils.logEventTracking(context, "result_share")
                Timber.i("moreContainer onClick...")
                imageSharingHelper.shareImageToApps(
                    requireContext(),
                    image = editImageViewModel.resultBitmap.value as Any
                )
            }
        }
    }

    override fun setupUi() {
        FirebaseEventUtils.logEventTracking(context, "result_view")

        binding?.let { it ->
            val image = editImageViewModel.resultBitmap.value
            if (image is Uri) {
                it.imgResult.setImageURI(image)
            } else if (image is Bitmap) {
                it.imgResult.setImageBitmap(image)
            } else {
                it.imgResult.loadImageDrawableWithCompress(R.drawable.img_result_test)
            }
        }
        imageSharingHelper = ImageSharingHelper(requireContext())


    }

    override fun renderUi() {
        binding?.run {
            imgFacebook.loadImageDrawable(R.drawable.img_facebook)
            imgInstagram.loadImageDrawable(R.drawable.img_instagram)
            imgX.loadImageDrawable(R.drawable.img_twitter)
            imgMessenger.loadImageDrawable(R.drawable.img_messenger)
            imgMore.loadImageDrawable(R.drawable.img_more)
        }
        if (firebaseRemoteConfig.getAdsKey(context, RemoteConfigAdsConst.NATIVE_RESULT_SHOW)) {
            binding?.frameNative?.root?.visible()
            adsHomeViewModel.adNativeSaveImage.collectIn(this) { adNativeState ->
                when (adNativeState) {
                    is NativeAdState.NativeAdData -> {
                        val adsBinding = getAdNativeMediumRectangleBinding()
                        adsHomeViewModel.createAdNativeSaveImage.showNativeAd(
                            frameAd = binding?.frameNative?.root,
                            adsBinding = adsBinding,
                            nativeAdValue = adNativeState.nativeAdValue,
                            subscribe = adsHomeViewModel.getAdNativeSaveImage(),
                            isConfigRatio = true,
                            isReload = true
                        )
                    }

                    is NativeAdState.NativeAdError -> {
                        binding?.frameNative?.root?.invisible()
                    }

                    else -> {}
                }
            }
        }
        val bitmap = editImageViewModel.resultBitmap.value
        if (bitmap == null) {
            toast(getString(R.string.not_found_bitmap))
            return
        }
        if (editImageViewModel.enableSaveImage) {
            viewLifecycleOwner.lifecycleScope.launch {
                val folder =
                    if (editImageViewModel.isTemplateFunction) TEMPLATE_FOLDER else COLLAGE_FOLDER
                val result = saveImageToStorage(
                    context = requireContext(),
                    pathName = "/${APP_FOLDER}/${folder}",
                    bitmap = bitmap as Bitmap,
                    fileName = System.currentTimeMillis().toString()
                )
                if (result) toast(getString(R.string.save_success)) else toast(getString(R.string.save_failure))
                editImageViewModel.enableRating()
                collageViewModel.enableSaveImage = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        editImageViewModel.enableSaveImage = false
        editImageViewModel.setResultBitmap(null)
    }

    override fun onDestroy() {
        adsHomeViewModel.createAdNativeSaveImage.destroy()
        super.onDestroy()
    }

}