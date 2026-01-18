package com.asoft.artsal.photo.ui.template.fragment

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.util.isNotEmpty
import androidx.core.util.size
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.FragmentEditorTemplateBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.customview.FilterTypeView
import com.asoft.artsal.photo.customview.MaskImageView
import com.asoft.artsal.photo.data.model.BaseData
import com.asoft.artsal.photo.data.model.BaseResponse
import com.asoft.artsal.photo.data.model.Filter
import com.asoft.artsal.photo.data.model.FilterType
import com.asoft.artsal.photo.data.model.FilterTypeItem
import com.asoft.artsal.photo.event.AppEventProvider
import com.asoft.artsal.photo.extensions.collectIn
import com.asoft.artsal.photo.extensions.downloadImageAsBitmap
import com.asoft.artsal.photo.extensions.getBitmapFromViewWithImageBounds
import com.asoft.artsal.photo.extensions.internal.getAdsKey
import com.asoft.artsal.photo.extensions.invisible
import com.asoft.artsal.photo.extensions.loadImageAssetsWithCompress
import com.asoft.artsal.photo.extensions.navigateToWithAnim
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.popBackStack
import com.asoft.artsal.photo.extensions.toast
import com.asoft.artsal.photo.extensions.visible
import com.asoft.artsal.photo.ui.ads.AdsHomeViewModel
import com.asoft.artsal.photo.ui.ai.viewmodel.ImageViewModel
import com.asoft.artsal.photo.ui.gallery.GalleryPhotoFragment.Companion.REQUEST_PHOTOS_KEY
import com.asoft.artsal.photo.ui.gallery.GalleryPhotoFragment.Companion.REQUEST_PHOTO_LIMITED_KEY
import com.asoft.artsal.photo.ui.gallery.GalleryPhotoFragment.Companion.RESULT_PHOTOS_KEY
import com.asoft.artsal.photo.ui.gallery.GalleryPhotoFragment.Companion.RESULT_PHOTO_LIMITED_KEY
import com.asoft.artsal.photo.ui.gallery.GalleryPhotoFragment.Companion.RESULT_TYPE
import com.asoft.artsal.photo.ui.gallery.GalleryPhotoFragment.Companion.TEMPLATE
import com.asoft.artsal.photo.ui.home.viewmodel.EditImageViewModel
import com.asoft.artsal.photo.ui.template.viewmodel.TemplateViewModel
import com.asoft.artsal.photo.utils.AdsIdUtils
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import com.asoft.artsal.photo.utils.RemoteConfigAdsConst
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.common.reflect.TypeToken
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.minsap.ad.ads.MinSapInterstitialAd
import com.minsap.ad.ads.common.InterstitialAdState
import com.minsap.ad.config.AdError
import com.minsap.ad.listener.BannerAdCallBack
import com.minsap.ad.listener.InterstitialAdCallBack
import com.t8rin.trickle.Trickle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class EditorTemplateFragment : BaseFragment<FragmentEditorTemplateBinding>(),
    MaskImageView.OnMaskImageViewClickListener {
    private val editImageViewModel: EditImageViewModel by activityViewModels<EditImageViewModel>()
    private val adsHomeViewModel: AdsHomeViewModel by activityViewModels<AdsHomeViewModel>()
    private val templateViewModel: TemplateViewModel by activityViewModels<TemplateViewModel>()
    private val imageViewModel: ImageViewModel by activityViewModels<ImageViewModel>()

    @Inject
    lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    private var currentImage: MaskImageView? = null

    private var filterTypeView: FilterTypeView? = null

    private var listMaskImage: MutableList<Pair<Int, MaskImageView>>? = null

    private var uriTmp: Uri? = null

    private var imageCount = 0

    private var cachedBitmap: Bitmap? = null

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentEditorTemplateBinding {
        return FragmentEditorTemplateBinding.inflate(inflater, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private fun setupTemplateLayout() {
        val container = binding?.templatePreview

        clearAllSelections()

//        val inflater = LayoutInflater.from(context)
//        val templateLayoutId =
//            templateViewModel.templateSelected.value?.layoutId ?: R.layout.temp_birthday_01
//        val templateLayout = TempTravel01Binding.inflate(layoutInflater)
//        templateLayout.imgLayer1.loadImageDrawableWithCompress(drawableRes = R.drawable.layer_first_travel_01, false)
//        templateLayout.imgLayer2.loadImageDrawableWithCompress(drawableRes = R.drawable.layer_second_travel_01, false)

        val inflater = LayoutInflater.from(context)

        val template = templateViewModel.templateSelected.value
        val templateLayoutId = template?.layoutId ?: R.layout.temp_birthday_02
//        val templateLayoutId = R.layout.temp_moment_09
        val templateLayout = inflater.inflate(templateLayoutId, container, false)


        if (listMaskImage == null) {
            listMaskImage = mutableListOf()
        }
        imageCount = 0
        listMaskImage?.clear()

        container?.removeAllViews()
        container?.addView(templateLayout)

        template?.layerFirstId?.let {
            templateLayout.findViewById<ImageView>(R.id.imgLayer1)
                ?.loadImageAssetsWithCompress(imagePath = it, enableCompression = false)
        }

        template?.layerSecondId?.let {
            templateLayout.findViewById<ImageView>(R.id.imgLayer2)
                ?.loadImageAssetsWithCompress(imagePath = it, enableCompression = false)
        }

        findAndSetupMaskImageViews(templateLayout)
        templateViewModel.imageCount = imageCount


        templateViewModel.updateIsSaveAvailable(false)

        if (templateViewModel.imageBackup.isNotEmpty()) {
            listMaskImage?.forEach { pair ->
                val idx = pair.first
                val view = pair.second
                val backupUri = templateViewModel.imageBackup.get(idx)
                if (backupUri != null) {
                    view.setImageWithCenterCrop(backupUri)
                    view.scaleType = ImageView.ScaleType.MATRIX
                    view.isFirstSelect = true
                }
            }

            if (templateViewModel.imageCount > 0 && templateViewModel.imageCount == templateViewModel.imageBackup.size) {
                templateViewModel.updateIsSaveAvailable(true)
            }
        }
    }

    private fun findAndSetupMaskImageViews(rootView: View) {
        when (rootView) {
            is MaskImageView -> {
                rootView.setOnMaskImageViewClickListener(this)
                imageCount++
                listMaskImage?.add(imageCount to rootView)
            }

            is ViewGroup -> {
                for (i in 0 until rootView.childCount) {
                    findAndSetupMaskImageViews(rootView.getChildAt(i))
                }
            }
        }
    }

    private fun clearAllSelections() {
        listMaskImage?.forEach { it.second.isSelected = false }
        currentImage = null
    }

    override fun initListener() {
        editImageViewModel.callCropSuccess.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                val out = editImageViewModel.outputUri

                editImageViewModel.setSelectedFilter(null)

                updateImageEdited(out)

                val croppedBitmap = getCurrentBitmapFromImageView()
                if (croppedBitmap != null) {
                    editImageViewModel.setBitmapCache(croppedBitmap)
                    editImageViewModel.setBitmapToFilter(croppedBitmap)
                }

                binding?.run {
                    viewHeader.root.visible()
                }
                templateViewModel.updateShowNav(true)
                listMaskImage?.forEach { pair ->
                    if (pair.first == templateViewModel.positionSelected) {
                        pair.second.isSelected = true
                    } else {
                        pair.second.isSelected = false
                    }
                }
                filterTypeView?.hide()
            }
        }


        binding?.run {
            viewHeader.btnBack.onClick {
                editImageViewModel.clearDataTemplate()
                templateViewModel.clearDataTemplate()
                popBackStack()
            }
            viewHeader.btnSave.run {
                onClick {
                    FirebaseEventUtils.logEventTracking(context, "edit_template_save")

                    if (it.alpha == 0.5f) {
                        toast(context.getString(R.string.please_add_image_to_save))
                    } else {
                        listMaskImage?.forEach { pair ->
                            pair.second.isSelected = false
                        }

                        editImageViewModel.apply {
                            setResultBitmap(
                                binding?.templatePreview?.getBitmapFromViewWithImageBounds(R.id.imgLayer1)
                            )
                            enableSaveImage = true
                        }
                        adsHomeViewModel.preloadInterAdSaveTemplate()
                    }
                }
            }
            menuCrop.onClick {
                FirebaseEventUtils.logEventTracking(context, "edit_template_click_crop")

                val selectedIndex = templateViewModel.positionSelected
                val filteredBitmap = editImageViewModel.bitmapToFilter.value
                val originalBitmap = editImageViewModel.bitmapCache.value

                if (filteredBitmap != null && filteredBitmap != originalBitmap) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val filteredUri =
                            templateViewModel.persistBitmapToCacheWithProvider(filteredBitmap)
                        if (filteredUri != null) {
                            templateViewModel.updateImageBackup(filteredUri)
                            editImageViewModel.apply {
                                loadBitmap(filteredUri, onLoadingStateChange = {
                                    Timber.i("callback load filtered bitmap for crop: $it")
                                })
                            }
                        } else {
                            val selectedUri =
                                templateViewModel.imageBackup.get(selectedIndex) ?: uriTmp
                            editImageViewModel.apply {
                                loadBitmap(selectedUri, onLoadingStateChange = {
                                    Timber.i("callback load backup bitmap: $it")
                                })
                            }
                        }
                    }
                } else {
                    val selectedUri = templateViewModel.imageBackup.get(selectedIndex) ?: uriTmp
                    editImageViewModel.apply {
                        loadBitmap(selectedUri, onLoadingStateChange = {
                            Timber.i("callback load bitmap: $it")
                        })
                    }
                }

                navigateToWithAnim(

                    id = R.id.action_editor_template_fragment_to_crop_fragment
                )
            }

            menuReplace.onClick {
                FirebaseEventUtils.logEventTracking(context, "edit_template_click_replace")

                navToGallery()
                currentImage?.isFirstSelect = true
            }
            navAi.run {
                btnClose.onClick {
                    navAi.root.invisible()

                }
                menuUpscale.onClick {
                    handleClickAi("upscale")
                }
                menuBeautify.onClick {
                    handleClickAi("beautify")
                }
                menuEnhance.onClick {
                    handleClickAi("enhance")
                }
            }

            menuAiEnhance.onClick {
                navAi.root.visible()
                FirebaseEventUtils.logEventTracking(context, "edit_template_click_ai_enhance")
            }

            menuFilter.onClick {
                FirebaseEventUtils.logEventTracking(context, "edit_template_click_filter")

                val base = getCurrentBitmapFromImageView()
                if (base != null) {
                    editImageViewModel.setBitmapCache(base)
                    editImageViewModel.setBitmapToFilter(base)
                }

                if (filterTypeView != null) {
                    try {
                        val parent = filterTypeView?.parent
                        if (parent == null) {
                            filterTypeView = null
                        } else {
                            filterTypeView?.show()
                            filterTypeView?.bringToFront()
                        }
                    } catch (e: Exception) {
                        binding?.root?.removeView(filterTypeView)
                        filterTypeView = null
                    }
                }

                listMaskImage?.forEach { pair ->
                    pair.second.isClickable = false
                    pair.second.isFocusable = false
                }

                if (filterTypeView == null) {
                    filterTypeView = FilterTypeView(requireContext()).apply {
                        layoutParams = ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.MATCH_PARENT,
                            ConstraintLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomToTop = R.id.bannerAds
                            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                        }
                        setOnFilterTypeListener(object : FilterTypeView.OnFilterTypeListener {
                            override fun onCloseClicked(isReset: Boolean) {
                                binding?.viewHeader?.root?.visible()
                                filterTypeView?.invisible()
                                showLoading(true)

                                if (isReset) {
                                    editImageViewModel.setSelectedFilter(null)
                                    editImageViewModel.bitmapCache.value?.let { originalBmp ->
                                        currentImage?.setImageBitmap(originalBmp)
                                        editImageViewModel.setBitmapToFilter(originalBmp)
                                    }
                                    cachedBitmap = null
                                    showLoading(false)
                                    Timber.d("Filter cancelled, restored original image")
                                } else {
                                    viewLifecycleOwner.lifecycleScope.launch {
                                        val filtered = editImageViewModel.bitmapToFilter.value
                                        if (filtered != null) {
                                            currentImage?.setImageBitmap(filtered)
                                            val uri =
                                                templateViewModel.persistBitmapToCacheWithProvider(
                                                    filtered
                                                )
                                            updateImageEdited(uri)

                                            editImageViewModel.setBitmapCache(filtered)
                                            cachedBitmap = null
                                            Timber.d("Filter committed and saved to backup: $uri")
                                        }
                                    }
                                }
                                listMaskImage?.forEach { pair ->
                                    pair.second.isClickable = true
                                    pair.second.isFocusable = true
                                }
                                showLoading(false)

                            }

                            override fun onFilterTypeSelected(filterType: FilterTypeItem) {
                                Timber.d("Filter type selected: ${filterType.type}")
                                val filters: List<Filter> = when (filterType.type) {
                                    FilterType.BAndW -> {
                                        FirebaseEventUtils.logEventTracking(context, "filter_bw")
                                        editImageViewModel.filterBAW
                                    }

                                    FilterType.Cinematic -> {
                                        FirebaseEventUtils.logEventTracking(
                                            context,
                                            "filter_cinematic"
                                        )
                                        editImageViewModel.filterCinematic
                                    }

                                    FilterType.Landscape -> {
                                        FirebaseEventUtils.logEventTracking(
                                            context,
                                            "filter_landscape"
                                        )
                                        editImageViewModel.filterLandscape
                                    }

                                    FilterType.LifeStyle -> {
                                        FirebaseEventUtils.logEventTracking(
                                            context,
                                            "filter_lifestyle"
                                        )
                                        editImageViewModel.filterLifeStyle
                                    }

                                    FilterType.Moody -> {
                                        FirebaseEventUtils.logEventTracking(context, "filter_moody")
                                        editImageViewModel.filterMoody
                                    }

                                    FilterType.Nature -> {
                                        FirebaseEventUtils.logEventTracking(
                                            context,
                                            "filter_nature"
                                        )
                                        editImageViewModel.filterNature
                                    }

                                    FilterType.Portrait -> {
                                        FirebaseEventUtils.logEventTracking(
                                            context,
                                            "filter_portrait"
                                        )
                                        editImageViewModel.filterPortrait
                                    }
                                }
                                filterTypeView?.setFilters(filters)

                                val currentSelectedFilter =
                                    editImageViewModel.getCurrentSelectedFilter()
                                if (currentSelectedFilter != null && filters.any { it.id == currentSelectedFilter.id }) {
//                                    binding?.postDelayed({
//                                        filterTypeView?.setCurrentSelectedFilter(
//                                            currentSelectedFilter,
//                                            shouldScroll = true
//                                        )
//                                    }, 100)
                                }
                            }

                            override fun onFilterSelected(filter: Filter) {
                                // Ensure base cache exists
                                if (editImageViewModel.bitmapCache.value == null) {
                                    getCurrentBitmapFromImageView()?.let { bmp ->
                                        editImageViewModel.setBitmapCache(bmp)
                                    }
                                }
                                editImageViewModel.setSelectedFilter(filter)
                                applySelectedFilter(filter)
                            }
                        })
                    }

                    binding?.root?.let { rootView ->
                        rootView.addView(filterTypeView)
                        filterTypeView?.bringToFront()
                    }
                    filterTypeView?.show()
                }
                binding?.viewHeader?.root?.invisible()

            }
        }
        adsHomeViewModel.isLoadingSaveTemplateDialog.observe(viewLifecycleOwner) {
            showLoadingAds(it)
        }
    }

    private fun handleClickAi(type: String) {
        val selectedIndex = templateViewModel.positionSelected
        val selectedUri = templateViewModel.imageBackup.get(selectedIndex) ?: uriTmp

        if (selectedUri == null) {
            toast("Vui lòng chọn ảnh trước")
            return
        }
        showLoading(true)

        when (type) {

            "enhance" -> {
                imageViewModel.enhanceImage(
                    imageUri = selectedUri,
                    scale = 2,
                    faceEnhance = null,
                    model = null
                )
            }

            "beautify" -> {
                imageViewModel.beautifyImage(
                    imageUri = selectedUri,
                    scale = 2
                )
            }

            "upscale" -> {
                imageViewModel.upscaleImage(
                    imageUri = selectedUri,
                    scale = 2,
                    version = "v1.4"
                )
            }

        }
    }

    private fun navToGallery() {
        setFragmentResult(
            REQUEST_PHOTO_LIMITED_KEY,
            bundleOf(
                RESULT_TYPE to TEMPLATE,
                RESULT_PHOTO_LIMITED_KEY to TEMPLATE_NUMBER_LIMITED
            )
        )
        navigateToWithAnim(R.id.action_editor_template_fragment_to_gallery_photo_fragment)
    }

    private fun updateImageEdited(out: Uri?) {
        val pos = templateViewModel.positionSelected
        if (out != null) {
            listMaskImage?.forEach { pair ->
                if (pair.first == pos) {
                    val maskImageView = pair.second
                    maskImageView.setImageWithCenterCrop(out)
                    maskImageView.scaleType = ImageView.ScaleType.MATRIX
                    maskImageView.isSelected = true
                    maskImageView.invalidate()
                    maskImageView.requestLayout()
                    currentImage = maskImageView
                    cachedBitmap = null
                    Timber.d("Updated image at position $pos with URI: $out")
                }
            }
            templateViewModel.updateImageBackup(out)
            uriTmp = out
            Timber.d("Image backup updated for position $pos")
        }
    }

    private fun getCurrentBitmapFromImageView(): Bitmap? {
        try {
            cachedBitmap?.let { return it }

            val iv = currentImage ?: return null
            val d = iv.drawable
            val bitmap = (d as? BitmapDrawable)?.bitmap

            cachedBitmap = bitmap
            return bitmap
        } catch (e: Exception) {
            FirebaseEventUtils.recordException(e)
            return null
        }
    }

    private fun loadBannerAds() {
        if (firebaseRemoteConfig.getAdsKey(context, RemoteConfigAdsConst.BANNER_TEMPLATE)) {
            binding?.bannerAds?.root?.visible()
            adsHomeViewModel.createBannerAdEditTemplate.loadAndShowBannerAd(
                activity,
                AdsIdUtils.BANNER_TEMPLATE,
                adName = "Template",
                frameAd = binding?.bannerAds?.root,
                callBackResult = object : BannerAdCallBack() {
                })
        }
    }

    override fun setupUi() {
        FirebaseEventUtils.logEventTracking(context, "edit_template_view")

        editImageViewModel.isTemplateFunction = true
        templateViewModel.updateIsSaveAvailable(false)
        setupTemplateLayout()
        setupCustomGradientColors()
        loadBannerAds()

    }

    private fun setupListenerUri() {
        setFragmentResultListener(
            REQUEST_PHOTOS_KEY
        ) { _, bundle ->
            try {
                val gson = Gson()
                val jsonString = bundle.getString(RESULT_PHOTOS_KEY)
                jsonString?.let { json ->
                    val type = object : TypeToken<List<String>>() {}.type
                    val uriStringList: List<String> = gson.fromJson(json, type)
                    val uriList = uriStringList.map { it.toUri() }
                    val uri: Uri? = uriList[0]
                    if (uri != null) {
                        uriTmp = uri
                        currentImage = currentImageSelected()
                        currentImage?.apply {
                            scaleType = ImageView.ScaleType.MATRIX
                            setImageWithCenterCrop(uri)
                            isFirstSelect = true
                            isSelected = true
                        }

                        templateViewModel.apply {
                            updateImageBackup(uri)
                            updateShowNav(true)
                        }
                    } else {
                        templateViewModel.updateShowNav(false)
                    }
                }
            } catch (e: Exception) {
                FirebaseEventUtils.recordException(e)
            }
        }
    }

    override fun renderUi() {
        // Observe enhance result
        imageViewModel.apply {
            enhanceResult.collectIn(this@EditorTemplateFragment, action = { result ->
                result?.let { handleEnhanceResult(it) }
            })
            enhanceError.collectIn(this@EditorTemplateFragment, action = { error ->
                error?.let {
                    showLoading(false)
                    toast("Lỗi xử lý ảnh: $it")
                    Timber.e("Enhance error: $it")
                }
            }
            )
            beautifyResult.collectIn(this@EditorTemplateFragment, action = { result ->
                result?.let { handleBeautifyResult(it) }
            })
            beautifyError.collectIn(this@EditorTemplateFragment, action = { error ->
                error?.let {
                    showLoading(false)
                    toast("Lỗi xử lý ảnh: $it")
                    Timber.e("Enhance error: $it")
                }
            })
            upscaleResult.collectIn(this@EditorTemplateFragment, action = { result ->
                result?.let {
                    handleUpscaleResult(it)
                }

            })
            upscaleError.collectIn(this@EditorTemplateFragment, action = {
                it?.let {
                    showLoading(false)
                    toast("Lỗi xử lý ảnh: $it")
                }
            })
        }


        adsHomeViewModel.adInterSaveTemplate.observe(viewLifecycleOwner) { interstitialAdState ->
            when (interstitialAdState) {
                is InterstitialAdState.InterstitialAdData -> {
                    showInterAd(
                        minsapInterstitialAd = adsHomeViewModel.createInterTemplate,
                        interstitialAd = interstitialAdState.interstitialAdValue.getAdValue()
                            ?: return@observe
                    )
                    adsHomeViewModel.preloadNativeAdSaveImage()
                }

                is InterstitialAdState.InterstitialAdError -> {
                    adsHomeViewModel.redirectSaveTemplateEvent.call(AppEventProvider.RedirectSavedResultScreen)
                }

                else -> {}
            }
        }
        if (templateViewModel.imageBackup.isNotEmpty()) {
            listMaskImage?.forEach { it ->
                it.second.apply {
                    setImageWithCenterCrop(templateViewModel.imageBackup.get(it.first))
                    scaleType = ImageView.ScaleType.MATRIX
                }
            }
        }
        templateViewModel.isShowNav.collectIn(
            this@EditorTemplateFragment,
            action = { it ->
                if (it) {
                    binding?.navigationBar?.visible()
                } else {
                    binding?.navigationBar?.invisible()
                }
            }
        )
    }

    private fun handleUpscaleResult(it: BaseResponse<BaseData>) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (it.status == "success" && it.data?.url != null) {
                    val imageUrl = it.data.url
                    Timber.d("Upscale success, downloading image from URL: $imageUrl")

                    // Download image from URL
                    val bitmap = downloadImageAsBitmap(requireContext(), imageUrl)

                    if (bitmap == null || bitmap.isRecycled) {
                        launch(Dispatchers.Main) {
                            showLoading(false)
                            toast("Không thể tải ảnh đã xử lý")
                        }
                        Timber.e("Failed to download upscaled image")
                        return@launch
                    }

                    Timber.d("Downloaded upscaled image, size: ${bitmap.width}x${bitmap.height}")

                    // Save bitmap to cache provider
                    val upscaledUri = templateViewModel.persistBitmapToCacheWithProvider(bitmap)

                    if (upscaledUri != null) {
                        Timber.d("Saved upscaled image to cache: $upscaledUri")

                        // Update image in MaskImageView with new URI on Main thread
                        launch(Dispatchers.Main) {
                            updateImageEdited(upscaledUri)
                            // Force view to refresh
                            currentImage?.invalidate()
                            currentImage?.requestLayout()

                            showLoading(false)
                            imageViewModel.clearUpscaleResult()

                            toast("Đã upscale ảnh thành công")
                            Timber.d("Upscaled image updated successfully")
                        }
                    } else {
                        launch(Dispatchers.Main) {
                            toast("Không thể lưu ảnh đã xử lý")
                        }
                        Timber.e("Failed to save upscaled image to cache")
                    }
                } else {
                    launch(Dispatchers.Main) {
                        toast("Upscale ảnh thất bại: ${it.status}")
                        showLoading(false)
                    }
                    Timber.e("Upscale failed with status: ${it.status}")
                }
            } catch (e: Exception) {
                FirebaseEventUtils.recordException(e)
                Timber.e(e, "Error handling upscale result")
                launch(Dispatchers.Main) {
                    toast("Lỗi khi xử lý ảnh: ${e.message}")
                    showLoading(false)
                }
            }
        }
    }

    private fun handleBeautifyResult(it: BaseResponse<BaseData>) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (it.status == "success" && it.data?.url != null) {
                    val imageUrl = it.data.url
                    Timber.d("Beautify success, downloading image from URL: $imageUrl")

                    // Download image from URL
                    val bitmap = downloadImageAsBitmap(requireContext(), imageUrl)

                    if (bitmap == null || bitmap.isRecycled) {
                        launch(Dispatchers.Main) {
                            showLoading(false)
                            toast("Không thể tải ảnh đã xử lý")
                        }
                        Timber.e("Failed to download beautified image")
                        return@launch
                    }

                    Timber.d("Downloaded beautified image, size: ${bitmap.width}x${bitmap.height}")

                    // Save bitmap to cache provider
                    val beautifiedUri =
                        templateViewModel.persistBitmapToCacheWithProvider(bitmap)

                    if (beautifiedUri != null) {
                        Timber.d("Saved beautified image to cache: $beautifiedUri")

                        // Update image in MaskImageView with new URI on Main thread
                        launch(Dispatchers.Main) {
                            updateImageEdited(beautifiedUri)
                            // Force view to refresh
                            currentImage?.invalidate()
                            currentImage?.requestLayout()

                            showLoading(false)
                            imageViewModel.clearBeautifyResult()

                            toast("Đã làm đẹp ảnh thành công")
                            Timber.d("Beautified image updated successfully")
                        }
                    } else {
                        launch(Dispatchers.Main) {
                            toast("Không thể lưu ảnh đã xử lý")
                        }
                        Timber.e("Failed to save beautified image to cache")
                    }
                } else {
                    launch(Dispatchers.Main) {
                        toast("Làm đẹp ảnh thất bại: ${it.status}")
                        showLoading(false)
                    }
                    Timber.e("Beautify failed with status: ${it.status}")
                }
            } catch (e: Exception) {
                FirebaseEventUtils.recordException(e)
                Timber.e(e, "Error handling beautify result")
                launch(Dispatchers.Main) {
                    toast("Lỗi khi xử lý ảnh: ${e.message}")
                    showLoading(false)
                }
            }
        }
    }

    private fun currentImageSelected(): MaskImageView? {
        return listMaskImage?.get(templateViewModel.positionSelected - 1)?.second
    }

    private fun showInterAd(
        minsapInterstitialAd: MinSapInterstitialAd, interstitialAd: InterstitialAd
    ) {
        minsapInterstitialAd.showInterstitialAd(
            context = activity,
            interstitialAd = interstitialAd,
            callBackShow = object : InterstitialAdCallBack() {
                override fun onAdDismissed() {
                    adsHomeViewModel.redirectSaveTemplateEvent.call(AppEventProvider.RedirectSavedResultScreen)
                }

                override fun onAdFailedToShow(error: AdError?) {
                    adsHomeViewModel.redirectSaveTemplateEvent.call(AppEventProvider.RedirectSavedResultScreen)
                }

                override fun onAdShown() {

                }
            })
    }

    override fun onMaskImageViewClick(e: MaskImageView?) {
        listMaskImage?.forEach { pair ->
            pair.second.isSelected = false
            if (pair.second == e) {
                templateViewModel.positionSelected = pair.first
            }
        }

        currentImage = e
        currentImage?.isSelected = true
        currentImage?.invalidate()

        editImageViewModel.setSelectedFilter(null)
        cachedBitmap = null


        if (currentImage?.hasUserImage() == false) {
            FirebaseEventUtils.logEventTracking(context, "edit_template_select_img")
            navToGallery()
        } else {
            val currentBitmap = getCurrentBitmapFromImageView()
            if (currentBitmap != null) {
                editImageViewModel.setBitmapCache(currentBitmap)
                editImageViewModel.setBitmapToFilter(currentBitmap)
            }
            templateViewModel.updateShowNav(true)
        }
    }

    override fun onResume() {
        super.onResume()
        setupListenerUri()
        adsHomeViewModel.redirectSaveTemplateEvent.observe(viewLifecycleOwner) {
            if (it is AppEventProvider.RedirectSavedResultScreen) {
                adsHomeViewModel.preloadNativeAdSaveImage()
                navigateToWithAnim(
                    id = R.id.action_editor_template_fragment_to_save_result_fragment,
                )
                adsHomeViewModel.getAdSaveTemplateResult().call(null)
            }
        }
        templateViewModel.isSaveAvailableState.collectIn(
            this@EditorTemplateFragment,
            action = {
                binding?.viewHeader?.btnSave?.run {
                    if (it) {
                        alpha = 1f
                    } else {
                        alpha = 0.5f
                    }
                }
            })

    }

    private fun applySelectedFilter(filter: Filter) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val base = editImageViewModel.bitmapCache.value ?: getCurrentBitmapFromImageView()
            if (base == null) return@launch

            try {
                val result = Trickle.applyCubeLut(base, filter.lutPath, 1f)

                // Update UI on main thread
                launch(Dispatchers.Main) {
                    currentImage?.setImageBitmap(result)
                    editImageViewModel.setBitmapToFilter(result)
                    // Invalidate cached bitmap since we applied a filter
                    cachedBitmap = null
                }
            } catch (e: Exception) {
                FirebaseEventUtils.recordException(e)
                launch(Dispatchers.Main) {
                    // Show error or fallback to original image
                    currentImage?.setImageBitmap(base)
                }
            } finally {

            }
        }
    }

    override fun handleBackPress() {
        templateViewModel.clearDataTemplate()
        editImageViewModel.clearDataTemplate()
        super.handleBackPress()
    }

    override fun onDestroyView() {
        filterTypeView?.let { view ->
            binding?.root?.removeView(view)
        }
        templateViewModel.apply {
            imageCount = 0
            updateIsSaveAvailable(false)
        }
        imageCount = 0
        filterTypeView = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        adsHomeViewModel.createBannerAdEditTemplate.destroy()
    }

    private fun setupCustomGradientColors() {
        val gradientColors = intArrayOf(
            "#DCA8CD".toColorInt(),
            "#A7B4DE".toColorInt(),
            "#608EE8".toColorInt()
        )

        listMaskImage?.forEach { pair ->
            pair.second.selectedGradientColors = gradientColors
            pair.second.setUseGradientBorder(true)
            pair.second.setSelectedStrokeWidth(8f)
        }
    }

    private fun handleEnhanceResult(response: BaseResponse<BaseData>) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (response.status == "success" && response.data?.url != null) {
                    val imageUrl = response.data.url
                    Timber.d("Enhance success, downloading image from URL: $imageUrl")

                    // Download image from URL
                    val bitmap = downloadImageAsBitmap(requireContext(), imageUrl)

                    if (bitmap == null || bitmap.isRecycled) {
                        launch(Dispatchers.Main) {
                            showLoading(false)
                            toast("Không thể tải ảnh đã xử lý")
                        }
                        Timber.e("Failed to download enhanced image")
                        return@launch
                    }

                    Timber.d("Downloaded enhanced image, size: ${bitmap.width}x${bitmap.height}")

                    // Save bitmap to cache provider
                    val enhancedUri = templateViewModel.persistBitmapToCacheWithProvider(bitmap)

                    if (enhancedUri != null) {
                        Timber.d("Saved enhanced image to cache: $enhancedUri")

                        // Update image in MaskImageView with new URI on Main thread
                        launch(Dispatchers.Main) {
                            updateImageEdited(enhancedUri)
                            // Force view to refresh
                            currentImage?.invalidate()
                            currentImage?.requestLayout()

                            showLoading(false)
                            imageViewModel.clearEnhanceResult()
                            toast("Đã xử lý ảnh thành công")
                            Timber.d("Enhanced image updated successfully")
                        }
                    } else {
                        launch(Dispatchers.Main) {
                            toast("Không thể lưu ảnh đã xử lý")
                        }
                        Timber.e("Failed to save enhanced image to cache")
                    }
                } else {
                    launch(Dispatchers.Main) {
                        toast("Xử lý ảnh thất bại: ${response.status}")
                        showLoading(false)
                    }
                    Timber.e("Enhance failed with status: ${response.status}")
                }
            } catch (e: Exception) {
                FirebaseEventUtils.recordException(e)
                Timber.e(e, "Error handling enhance result")
                launch(Dispatchers.Main) {
                    toast("Lỗi khi xử lý ảnh: ${e.message}")
                    showLoading(false)
                }
            }
        }
    }

    companion object {
        const val TEMPLATE_NUMBER_LIMITED = 1
    }
}