package com.asoft.artsal.photo.ui.collage.fragment

import android.graphics.Bitmap
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.fragment.app.activityViewModels
import com.artsal.photo.editor.collage.maker.databinding.FragmentCropBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.extensions.internal.getAdsKey
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.popBackStack
import com.asoft.artsal.photo.extensions.visible
import com.asoft.artsal.photo.ui.collage.viewmodel.CollageViewModel
import com.asoft.artsal.photo.ui.home.viewmodel.EditImageViewModel
import com.asoft.artsal.photo.utils.AdsIdUtils
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import com.asoft.artsal.photo.utils.RemoteConfigAdsConst
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.minsap.ad.ads.MinSapBannerAd
import com.minsap.ad.listener.BannerAdCallBack
import com.yalantis.ucrop.callback.BitmapCropCallback
import com.yalantis.ucrop.view.OverlayView
import com.yalantis.ucrop.view.TransformImageView
import com.yalantis.ucrop.view.UCropView
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class CropFragment : BaseFragment<FragmentCropBinding>() {

    @Inject
    lateinit var firebaseRemoteConfig: FirebaseRemoteConfig

    private val createBannerAd by lazy { MinSapBannerAd.create() }

    private val editImageViewModel: EditImageViewModel by activityViewModels<EditImageViewModel>()

    private val collageViewModel: CollageViewModel by activityViewModels<CollageViewModel>()

    private lateinit var uCropView: UCropView
    private var currentAspectRatio = 0f

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCropBinding {
        return FragmentCropBinding.inflate(layoutInflater, container, false)
    }

    override fun initListener() {
        binding?.apply {
            header.imgClose.onClick {

                FirebaseEventUtils.logEventTracking(context, "crop_close")
                popBackStack()
            }

            header.imgDone.onClick {
                FirebaseEventUtils.logEventTracking(context, "crop_save")
                cropAndSaveImage()
            }

            control.btnCustom.onClick {
                FirebaseEventUtils.logEventTracking(context, "crop_custom")

                uCropView.overlayView.setFreestyleCropMode(OverlayView.FREESTYLE_CROP_MODE_ENABLE_WITH_PASS_THROUGH)
                setAspectRatio(0f)
            }

            control.btnRadio11.onClick {
                FirebaseEventUtils.logEventTracking(context, "crop_1_1")

                uCropView.overlayView.setFreestyleCropMode(OverlayView.FREESTYLE_CROP_MODE_DISABLE)
                setAspectRatio(1f)
            }

            control.btnRadio34.onClick {
                FirebaseEventUtils.logEventTracking(context, "crop_3_4")

                uCropView.overlayView.setFreestyleCropMode(OverlayView.FREESTYLE_CROP_MODE_DISABLE)
                setAspectRatio(3f / 4f)
            }

            control.btnRadio32.onClick {
                FirebaseEventUtils.logEventTracking(context, "crop_3_2")

                uCropView.overlayView.setFreestyleCropMode(OverlayView.FREESTYLE_CROP_MODE_DISABLE)
                setAspectRatio(3f / 2f)
            }

            control.btnRadio169.onClick {
                FirebaseEventUtils.logEventTracking(context, "crop_16_9")

                uCropView.overlayView.setFreestyleCropMode(OverlayView.FREESTYLE_CROP_MODE_DISABLE)
                setAspectRatio(16f / 9f)
            }

            control.btnRadio916.onClick {
                FirebaseEventUtils.logEventTracking(context, "crop_9_16")

                uCropView.overlayView.setFreestyleCropMode(OverlayView.FREESTYLE_CROP_MODE_DISABLE)
                setAspectRatio(9f / 16f)
            }

            control.btnRotate.onClick {
                rotateImage()
            }
        }
    }

    override fun setupUi() {
        loadBannerAds()
        setupUCropView()
        loadImageFromCropCache()

        uCropView.alpha = 1f
        uCropView.visibility = android.view.View.VISIBLE

        uCropView.overlayView.apply {
            setDimmedColor("#8CE6E6E6".toColorInt())
            setShowCropFrame(true)
            setShowCropGrid(true)
            setFreestyleCropMode(OverlayView.FREESTYLE_CROP_MODE_ENABLE_WITH_PASS_THROUGH)
        }
    }

    override fun renderUi() {
        setAspectRatio(0f)
    }

    private fun setupUCropView() {
        uCropView = binding?.ucrop ?: run {
            return
        }
        uCropView.cropImageView.setTransformImageListener(object :
            TransformImageView.TransformImageListener {
            override fun onRotate(currentAngle: Float) {
                Timber.d("Image rotated...: $currentAngle")
            }

            override fun onScale(currentScale: Float) {
                Timber.d("Image scaled...: $currentScale")
            }

            override fun onLoadComplete() {
                Timber.d("Image load complete....")
            }

            override fun onLoadFailure(e: Exception) {
                Timber.e(e, "Image load failed")
                FirebaseEventUtils.recordException(e)
            }
        })
    }

    private fun loadImageFromCropCache() {
        if (editImageViewModel.inputUri != Uri.EMPTY && editImageViewModel.outputUri != Uri.EMPTY) {
            try {
                uCropView.cropImageView.setImageUri(
                    editImageViewModel.inputUri ?: return,
                    editImageViewModel.outputUri
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading image from URI")
                FirebaseEventUtils.recordException(e)
            }
        }
    }

    private fun setAspectRatio(ratio: Float) {
        currentAspectRatio = ratio
        uCropView.cropImageView.setTargetAspectRatio(ratio)

        binding?.control?.apply {
            btnCustom.alpha = 0.5f
            btnRadio11.alpha = 0.5f
            btnRadio34.alpha = 0.5f
            btnRadio32.alpha = 0.5f
            btnRadio169.alpha = 0.5f
            btnRadio916.alpha = 0.5f

            when (ratio) {
                0f -> btnCustom.alpha = 1.0f
                1f -> btnRadio11.alpha = 1.0f
                3f / 4f -> btnRadio34.alpha = 1.0f
                3f / 2f -> btnRadio32.alpha = 1.0f
                16f / 9f -> btnRadio169.alpha = 1.0f
                9f / 16f -> btnRadio916.alpha = 1.0f
            }
        }

        Timber.d("Aspect ratio set to: $ratio")
    }

    private fun rotateImage() {
        FirebaseEventUtils.logEventTracking(context, "crop_rotate")
        uCropView.cropImageView.postRotate(90f)
    }

    private fun cropAndSaveImage() {
        try {
            showLoading(true)
            uCropView.cropImageView.cropAndSaveImage(
                Bitmap.CompressFormat.PNG,
                100,
                object : BitmapCropCallback {
                    override fun onBitmapCropped(
                        resultUri: Uri,
                        offsetX: Int,
                        offsetY: Int,
                        imageWidth: Int,
                        imageHeight: Int
                    ) {
                        Timber.d("Crop successful: $resultUri")
                        if(isAdded.not()) return
                        editImageViewModel.outputUri = resultUri
                        if(!editImageViewModel.isTemplateFunction) {
                            collageViewModel.uriTmp = resultUri
                            if (editImageViewModel.inputUri != null && editImageViewModel.outputUri != null) {
                                collageViewModel.updateImageByUri(
                                    editImageViewModel.inputUri ?: return,
                                    editImageViewModel.outputUri ?: return
                                )
                            } else {
                                Timber.w("CropFragment: onClick done: No input or output URI set")
                            }
                        } else {
                            editImageViewModel.callCropSuccess.call(true)
                        }
                        showLoading(false)
                        popBackStack()
                    }

                    override fun onCropFailure(throwable: Throwable) {
                        showLoading(false)
                        FirebaseEventUtils.recordException(Exception(throwable.message))
                        Timber.e(throwable, "Native crop failed, trying fallback method")
                    }
                }
            )
        } catch (e: Exception) {
            FirebaseEventUtils.recordException(e)

        }
    }

    override fun onDestroyView() {
        createBannerAd.destroy()
        editImageViewModel.inputUri = null
        editImageViewModel.outputUri = null
        super.onDestroyView()
    }

    private fun loadBannerAds() {
        if (firebaseRemoteConfig.getAdsKey(context, RemoteConfigAdsConst.BANNER_COLLAGE_SHOW)) {
            binding?.bannerAds?.root?.visible()
            createBannerAd.loadAndShowBannerAd(
                activity,
                AdsIdUtils.BANNER_COLLAGE,
                adName = "Collage",
                frameAd = binding?.bannerAds?.root,
                callBackResult = object : BannerAdCallBack() {

                })
        }
    }
}