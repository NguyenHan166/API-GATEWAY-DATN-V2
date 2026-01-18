package com.asoft.artsal.photo.ui.collage.fragment

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.appcompat.widget.AppCompatEditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.FragmentEditorCollageBinding
import com.artsal.photo.editor.collage.maker.databinding.FragmentStickerBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.business.collage.customview.FrameImageView
import com.asoft.artsal.photo.business.collage.customview.FramePhotoLayout
import com.asoft.artsal.photo.business.collage.model.PhotoItem
import com.asoft.artsal.photo.business.text.OnPhotoEditorListener
import com.asoft.artsal.photo.business.text.PhotoEditor
import com.asoft.artsal.photo.business.text.TextStyleBuilder
import com.asoft.artsal.photo.business.text.ViewType
import com.asoft.artsal.photo.component.GridSpacingItemDecoration
import com.asoft.artsal.photo.customview.CollageImageEditNavView
import com.asoft.artsal.photo.customview.FilterTypeView
import com.asoft.artsal.photo.data.model.Filter
import com.asoft.artsal.photo.data.model.FilterType
import com.asoft.artsal.photo.data.model.FilterTypeItem
import com.asoft.artsal.photo.event.AppEventProvider
import com.asoft.artsal.photo.extensions.collectIn
import com.asoft.artsal.photo.extensions.dpToPx
import com.asoft.artsal.photo.extensions.gone
import com.asoft.artsal.photo.extensions.initRecyclerViewAdapter
import com.asoft.artsal.photo.extensions.internal.getAdsKey
import com.asoft.artsal.photo.extensions.internal.getSizeByType
import com.asoft.artsal.photo.extensions.invisible
import com.asoft.artsal.photo.extensions.navigateToWithAnim
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.popBackStack
import com.asoft.artsal.photo.extensions.toast
import com.asoft.artsal.photo.extensions.visible
import com.asoft.artsal.photo.ui.ads.AdsHomeViewModel
import com.asoft.artsal.photo.ui.collage.adapter.StickerAdapter
import com.asoft.artsal.photo.ui.collage.bottomsheet.BackgroundBottomSheetFragment
import com.asoft.artsal.photo.ui.collage.bottomsheet.BorderBottomSheetFragment
import com.asoft.artsal.photo.ui.collage.bottomsheet.TextEditorBSFragment
import com.asoft.artsal.photo.ui.collage.viewmodel.CollageViewModel
import com.asoft.artsal.photo.ui.gallery.GalleryPhotoFragment.Companion.COLLAGE
import com.asoft.artsal.photo.ui.gallery.GalleryPhotoFragment.Companion.REQUEST_PHOTOS_KEY
import com.asoft.artsal.photo.ui.gallery.GalleryPhotoFragment.Companion.REQUEST_PHOTO_LIMITED_KEY
import com.asoft.artsal.photo.ui.gallery.GalleryPhotoFragment.Companion.RESULT_PHOTOS_KEY
import com.asoft.artsal.photo.ui.gallery.GalleryPhotoFragment.Companion.RESULT_PHOTO_LIMITED_KEY
import com.asoft.artsal.photo.ui.gallery.GalleryPhotoFragment.Companion.RESULT_TYPE
import com.asoft.artsal.photo.ui.home.viewmodel.EditImageViewModel
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
class EditorCollageFragment : BaseFragment<FragmentEditorCollageBinding>(), OnPhotoEditorListener,
    CollageImageEditNavView.OnImageEditNavListener {
    override val isInsets: Boolean
        get() = true
    private val stickerAdapter by lazy {
        StickerAdapter { selectedSticker ->
            onStickerSelected(selectedSticker)
        }
    }
    private lateinit var framePhotoLayout: FramePhotoLayout

    @Inject
    lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    private val editImageViewModel: EditImageViewModel by activityViewModels<EditImageViewModel>()
    private val collageViewModel: CollageViewModel by activityViewModels<CollageViewModel>()
    private val adsHomeViewModel: AdsHomeViewModel by activityViewModels<AdsHomeViewModel>()
    private var handlerBindView: Handler? = null
    private var stickerBottomSheet: FragmentStickerBinding? = null
    private var filterTypeView: FilterTypeView? = null
    private var photoEditor: PhotoEditor? = null
    private var selectedTextView: View? = null
    private var editTextCurrent: AppCompatEditText? = null
    private var isPhotoEditorReady = false
    private var isAddStickerTmp = false
    private var firstGuideSwap = true
    private var onGlobalListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    override fun onInflateView(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentEditorCollageBinding {
        return FragmentEditorCollageBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun initListener() {

        setupListenerUri()

        binding?.apply {
            viewHeader.btnSave.onClick {
                FirebaseEventUtils.logEventTracking(context, "edit_collage_save")

                photoEditor?.clearHelperBox()
                if (::framePhotoLayout.isInitialized) {
                    framePhotoLayout.clearSelection()
                }
                val finalBitmap = buildFinalBitmap() ?: run {
                    toast(getString(R.string.cannot_build_image))
                    return@onClick
                }
                editImageViewModel.setResultBitmap(finalBitmap)
                editImageViewModel.enableSaveImage = true
                adsHomeViewModel.preloadInterAdSaveCollage()
            }
            menuBorder.onClick {
                FirebaseEventUtils.logEventTracking(context, "edit_collage_click_border")

                BorderBottomSheetFragment.newInstance().apply {
                    isCancelable = false
                }.show(
                    activity?.supportFragmentManager ?: return@onClick,
                    BorderBottomSheetFragment::class.java.simpleName
                )
                viewHeader.root.invisible()
            }
            menuBackground.onClick {
                FirebaseEventUtils.logEventTracking(context, "edit_collage_click_bg")

                BackgroundBottomSheetFragment.newInstance().apply {
                    isCancelable = false
                }.show(
                    activity?.supportFragmentManager ?: return@onClick,
                    BackgroundBottomSheetFragment::class.java.simpleName
                )
                viewHeader.root.invisible()
            }
            menuText.onClick {
                FirebaseEventUtils.logEventTracking(context, "edit_collage_click_text")
                FirebaseEventUtils.logEventTracking(context, "text_apply")

                if (collageViewModel.currentTextNumber <= 5) {
                    binding?.viewHeader?.root?.invisible()
                    val textEditorBSFragment = TextEditorBSFragment
                        .newInstance().apply {
                            isCancelable = false
                        }
                    textEditorBSFragment.show(
                        activity?.supportFragmentManager ?: return@onClick,
                        TextEditorBSFragment::class.java.simpleName
                    )
                    textEditorBSFragment.setupTextListener(object :
                        TextEditorBSFragment.TextEditorListener {
                        override fun onDone(inputText: String) {
                            if (!isPhotoEditorReady || photoEditor == null) {
                                return
                            }
                            if (inputText.isEmpty() && inputText.isBlank()) return
                            collageViewModel.currentTextNumber++
                            photoEditor?.addText(inputText, getStyleBuilder())
                            collageViewModel.resetText()
                        }

                        override fun onClose() {

                        }
                    })
                    viewHeader.root.invisible()
                } else {
                    toast(getString(R.string.message_limit_text))
                    return@onClick
                }
            }
            menuSticker.onClick {
                FirebaseEventUtils.logEventTracking(context, "edit_collage_click_sticker")

                if (stickerBottomSheet != null) {
                    stickerBottomSheet?.root?.visible()
                } else {
                    stickerBottomSheet = FragmentStickerBinding.inflate(
                        layoutInflater, binding?.root, false
                    )
                    val layoutParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                        startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    }

                    stickerBottomSheet?.root?.layoutParams = layoutParams

                    binding?.viewRoot?.addView(stickerBottomSheet?.root)
                    stickerBottomSheet?.root?.visible()
                    stickerBottomSheet?.rvSticker?.initRecyclerViewAdapter(
                        yourAdapter = stickerAdapter,
                        yourLayoutManager = GridLayoutManager(context ?: return@onClick, 4),
                        fixedSize = true,
                    )

                    stickerBottomSheet?.rvSticker?.addItemDecoration(
                        GridSpacingItemDecoration(
                            4, 12.dpToPx(), 8.dpToPx(), false, 0
                        )
                    )
                    val stickers = collageViewModel.stickers
                    stickerAdapter.submitList(stickers)
                    stickerBSListener()

                }
                viewHeader.root.invisible()
            }

            viewHeader.btnBack.onClick {
                FirebaseEventUtils.logEventTracking(context, "edit_collage_back")
                collageViewModel.clearDataTmp()
                popBackStack(R.id.collage_fragment)
            }
        }

        adsHomeViewModel.isLoadingStartSaveDialog.observe(viewLifecycleOwner) {
            showLoadingAds(it)
        }

        collageViewModel.showViewHeader.observe(viewLifecycleOwner) {
            binding?.viewHeader?.root?.visible()
        }

        binding?.imageEditNavView?.setOnImageEditNavListener(this)
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
                    collageViewModel.apply {
                        if (uriList.size == 1) {
                            updateImageByUri(
                                editImageViewModel.inputUri ?: return@apply,
                                uriList[0]
                            )
                        } else {
                            setImages(uriList)
                        }
                    }
                }

            } catch (e: Exception) {
                FirebaseEventUtils.recordException(e)
            }
        }
    }

    override fun handleBackPress() {
        collageViewModel.clearDataTmp()
        popBackStack(R.id.collage_fragment)
    }

    private fun showInterAd(
        minsapInterstitialAd: MinSapInterstitialAd, interstitialAd: InterstitialAd
    ) {
        minsapInterstitialAd.showInterstitialAd(
            context = activity,
            interstitialAd = interstitialAd,
            callBackShow = object : InterstitialAdCallBack() {
                override fun onAdDismissed() {
                    adsHomeViewModel.redirectSaveResultEvent.call(AppEventProvider.RedirectSavedResultScreen)
                }

                override fun onAdFailedToShow(error: AdError?) {
                    adsHomeViewModel.redirectSaveResultEvent.call(AppEventProvider.RedirectSavedResultScreen)
                }

                override fun onAdShown() {

                }
            })
    }

    private fun stickerBSListener() {

        stickerBottomSheet?.viewHeader?.run {
            imgDone.onClick {
                collageViewModel.numberStickerPerRound = 0
                isAddStickerTmp = false
                stickerBottomSheet?.root?.gone()
                collageViewModel.showViewHeader.call(true)
            }
            imgClose.onClick {
                if (isAddStickerTmp) {
                    while (collageViewModel.numberStickerPerRound != 0) {
                        collageViewModel.numberStickerPerRound--
                        if (collageViewModel.numberStickerPerRound < 0) collageViewModel.numberStickerPerRound =
                            0
                        photoEditor?.undo()
                    }
                }
                stickerBottomSheet?.root?.gone()
                collageViewModel.showViewHeader.call(true)
            }
        }
    }

    private fun getStyleBuilder(): TextStyleBuilder {
        val defaultTextStyle = collageViewModel.getTextStyle()
        val styleBuilder = TextStyleBuilder()
        styleBuilder.withTextColor(defaultTextStyle.textColor)
        try {
            val typeface =
                ResourcesCompat.getFont(requireContext(), defaultTextStyle.fontStyle.font)
            if (typeface != null) {
                styleBuilder.withTextFont(typeface)
                styleBuilder.withFontResourceId(defaultTextStyle.fontStyle.font)
            }
        } catch (e: Exception) {
            FirebaseEventUtils.recordException(e)
        }
        return styleBuilder
    }

    @Suppress("DEPRECATION")
    override fun setupUi() {
        FirebaseEventUtils.logEventTracking(context, "edit_collage_view")
        editImageViewModel.isTemplateFunction = false
        handlerBindView = Handler(Looper.getMainLooper())
    }

    override fun renderUi() {
        loadBannerAds()
        adsHomeViewModel.adInterSaveCollage.observe(viewLifecycleOwner) { interstitialAdState ->
            when (interstitialAdState) {
                is InterstitialAdState.InterstitialAdData -> {
                    showInterAd(
                        minsapInterstitialAd = adsHomeViewModel.createInterCollage,
                        interstitialAd = interstitialAdState.interstitialAdValue.getAdValue()
                            ?: return@observe
                    )
                    adsHomeViewModel.preloadNativeAdSaveImage()
                }

                is InterstitialAdState.InterstitialAdError -> {
                    adsHomeViewModel.redirectSaveResultEvent.call(AppEventProvider.RedirectSavedResultScreen)
                }

                else -> {}
            }
        }
        collageViewModel.apply {
            backgroundColor.collectIn(
                this@EditorCollageFragment, Lifecycle.State.STARTED,
            ) {
                if (::framePhotoLayout.isInitialized) {
                    framePhotoLayout.setBackgroundColorWithDrawable(it)
                }
            }
            border.collectIn(
                this@EditorCollageFragment, Lifecycle.State.STARTED
            ) { border ->
                if (::framePhotoLayout.isInitialized) {
                    framePhotoLayout.setSpace(border.borderOuter, border.borderRadius)
                }
            }
            images.collectIn(
                this@EditorCollageFragment, Lifecycle.State.STARTED
            ) {
                if (it != null) {
                    collageViewModel.updateImageMapped(it)
                }
            }
        }
        collageViewModel.imagesMapped.collectIn(
            this@EditorCollageFragment, Lifecycle.State.CREATED
        ) { imagesMapped ->
            Timber.i("imagesMapped: $imagesMapped")
            if (::framePhotoLayout.isInitialized) {
                binding?.root?.removeView(framePhotoLayout)
                framePhotoLayout.destroy()
            }
            framePhotoLayout = FramePhotoLayout(requireContext(), imagesMapped).apply {
                id = View.generateViewId()
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_CONSTRAINT, 0
                ).apply {
                    topToBottom = R.id.viewHeader
                    bottomToBottom = R.id.containerPreview
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    val sizeByType = getSizeByType(collageViewModel.viewType.value)
                    setMargins(
                        sizeByType.left, sizeByType.top, sizeByType.right, sizeByType.bottom
                    )
                }
                visibility = View.VISIBLE
                onGlobalListener = object :
                    ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                        val width = this@apply.width
                        val height = this@apply.height
                        if (width > 0 && height > 0) {
                            val setting = collageViewModel.getBorder()
                            build(
                                viewWidth = width,
                                viewHeight = height,
                                outputScaleRatio = 1f,
                                space = setting.borderOuter,
                                corner = setting.borderRadius
                            )
                        }
                    }
                }
                binding?.root?.viewTreeObserver?.addOnGlobalLayoutListener(onGlobalListener)
            }
            binding?.root?.addView(framePhotoLayout)

            if (::framePhotoLayout.isInitialized) {
                Timber.d("Initializing PhotoEditor...")
                photoEditor = PhotoEditor.Builder(requireContext(), framePhotoLayout)
                    .setPinchTextScalable(true) // set flag to make text scalable when pinch
                    .setImageView(framePhotoLayout.source ?: ImageView(context))
                    .build() // build photo editor sdk

                photoEditor?.setOnPhotoEditorListener(this)
                isPhotoEditorReady = true

            } else {
                Timber.e("framePhotoLayout not initialized!")
            }
            framePhotoLayout.setOnPhotoSelectedListener(object :
                FramePhotoLayout.OnSelectionAwareListener {
                override fun onPhotoSelected(
                    photoItem: PhotoItem, view: FrameImageView
                ) {
                    FirebaseEventUtils.logEventTracking(context, "edit_collage_click_img")
                    editImageViewModel.inputUri = photoItem.imagePath
                    view.image?.let { bitmap ->
                        editImageViewModel.setBitmapCache(bitmap)
                    }

                    photoEditor?.clearHelperBox()
                    editTextCurrent?.clearFocus()

                    if (collageViewModel.swapMode.value) {
                        val firstImage = collageViewModel.firstImageForSwap.value
                        if (firstImage != null && firstImage != photoItem) {
                            framePhotoLayout.swapImages(firstImage, photoItem)
                            collageViewModel.clearSwapState()
                        } else {
                            toast(getString(R.string.mes_select_same_image))
                        }
                    } else {
                        binding?.imageEditNavView?.show(photoItem, view)
                        binding?.navigationBar?.invisible()
                    }
                }

                override fun onSelectionCleared() {
                    binding?.imageEditNavView?.hide()
                    binding?.navigationBar?.visible()
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        adsHomeViewModel.redirectSaveResultEvent.observe(viewLifecycleOwner) {
            if (it is AppEventProvider.RedirectSavedResultScreen) {
                adsHomeViewModel.preloadNativeAdSaveImage()
                redirectNextScreen()
                adsHomeViewModel.getAdSaveCollageResult().call(null)
            }
        }
    }

    private fun loadBannerAds() {
        if (firebaseRemoteConfig.getAdsKey(context, RemoteConfigAdsConst.BANNER_COLLAGE_SHOW)) {
            binding?.bannerAds?.root?.visible()
            adsHomeViewModel.createBannerAdEditCollage.loadAndShowBannerAd(
                activity,
                AdsIdUtils.BANNER_COLLAGE,
                adName = "Collage",
                frameAd = binding?.bannerAds?.root,
                callBackResult = object : BannerAdCallBack() {
                })
        }
    }

    private fun onStickerSelected(sticker: Bitmap) {
        if (collageViewModel.currentStickerNumber <= 29) {
            isAddStickerTmp = true
            photoEditor?.addImage(sticker)
            collageViewModel.currentStickerNumber++
            collageViewModel.numberStickerPerRound++
        } else {
            toast(getString(R.string.message_sticker_limit))
        }
    }

    override fun onEditTextChangeListener(
        rootView: View, text: String, colorCode: Int, fontStyle: Int?
    ) {

        if (rootView.tag == ViewType.TEXT) {
            binding?.viewHeader?.root?.invisible()
            collageViewModel.apply {
                setText(text)
                setFontStyle(fontStyle)
                setTextColor(colorCode)
            }
            val textEditorBSFragment = TextEditorBSFragment.newInstance().apply {
                isCancelable = false
            }
            textEditorBSFragment.show(
                activity?.supportFragmentManager ?: return,
                TextEditorBSFragment::class.java.simpleName
            )
            textEditorBSFragment.setupTextListener(object :
                TextEditorBSFragment.TextEditorListener {
                override fun onDone(inputText: String) {
                    if (inputText.isEmpty() && inputText.isBlank()) return
                    val styleBuilder = getStyleBuilder()
                    photoEditor?.editText(
                        view = rootView,
                        inputText = inputText,
                        styleBuilder = styleBuilder
                    )
                    collageViewModel.resetText()
                }

                override fun onClose() {
                }
            })
        }
    }

    override fun onAddViewListener(
        viewType: ViewType, numberOfAddedViews: Int
    ) {
    }

    override fun onRemoveViewListener(
        viewType: ViewType, numberOfAddedViews: Int
    ) {
        when (viewType) {
            ViewType.TEXT -> {
                collageViewModel.showViewHeader.call(true)
                collageViewModel.currentTextNumber--
                if (numberOfAddedViews == 0) {
                    selectedTextView = null
                    editTextCurrent = null
                }

            }

            ViewType.IMAGE -> {
                collageViewModel.currentStickerNumber--

            }

            else -> {}
        }
    }

    override fun onStartViewChangeListener(viewType: ViewType) {
    }

    override fun onStopViewChangeListener(viewType: ViewType) {

    }

    override fun onTouchSourceImage(event: MotionEvent) {

    }

    private fun redirectNextScreen() {
        navigateToWithAnim(
            id = R.id.action_editor_collage_to_save_result_fragment,
        )
    }

    private fun buildFinalBitmap(): Bitmap? {
        try {
            if (!::framePhotoLayout.isInitialized) return null
            val collageBitmap = framePhotoLayout.createImage()
            if (collageBitmap == null) return null
            val resultBitmap = createBitmap(collageBitmap.width, collageBitmap.height)
            resultBitmap.applyCanvas {
                drawBitmap(collageBitmap, 0f, 0f, null)
                framePhotoLayout.draw(this)
            }
            return resultBitmap
        } catch (e: Exception) {
            FirebaseEventUtils.recordException(e)
            return null
        }
    }

    private fun clearSwapMode() {
        if (collageViewModel.swapMode.value) {
            collageViewModel.clearSwapState()
        }
    }

    override fun onCloseClicked() {
        binding?.imageEditNavView?.hide()
        binding?.navigationBar?.visible()
        if (::framePhotoLayout.isInitialized) {
            framePhotoLayout.clearSelection()
        }
        clearSwapMode()
    }

    override fun onSwapClicked(photoItem: PhotoItem, imageView: FrameImageView) {
        if (collageViewModel.swapMode.value) {
            val firstImage = collageViewModel.firstImageForSwap.value
            if (firstImage != null && firstImage != photoItem) {
                framePhotoLayout.swapImages(firstImage, photoItem)
                collageViewModel.setFirstImageForSwap(photoItem)

            } else {
                collageViewModel.clearSwapState()
            }
        } else {
            collageViewModel.enableSwapMode()
            collageViewModel.setFirstImageForSwap(photoItem)
            if (firstGuideSwap) {
                toast(getString(R.string.message_select_image))
                firstGuideSwap = false
            }
        }
    }

    override fun onCropClicked(photoItem: PhotoItem, imageView: FrameImageView) {
        clearSwapMode()
        editImageViewModel.loadBitmap(photoItem.imagePath, onLoadingStateChange = {
            Timber.i("callback load bitmap: $it")
        })

        handlerBindView?.postDelayed({
            navigateToWithAnim(
                id = R.id.action_editor_collage_to_crop_fragment
            )
        }, 50L)
    }

    override fun onReplaceClicked(photoItem: PhotoItem, imageView: FrameImageView) {
        clearSwapMode()
        setFragmentResult(
            REQUEST_PHOTO_LIMITED_KEY,
            bundleOf(
                RESULT_TYPE to COLLAGE,
                RESULT_PHOTO_LIMITED_KEY to COLLAGE_NUMBER_LIMITED
            )
        )
        navigateToWithAnim(R.id.action_editor_collage_fragment_to_gallery_photo_fragment)
    }

    override fun onFilterClicked(photoItem: PhotoItem, imageView: FrameImageView) {
        clearSwapMode()

        if (filterTypeView != null) {
            filterTypeView?.show()
        } else {
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
                        filterTypeView?.hide()
                        if (isReset) {
                            editImageViewModel.setSelectedFilter(null)
                            framePhotoLayout.updateSelectedImageWithFilter(
                                editImageViewModel.bitmapCache.value ?: return
                            )
                        } else {
                            editImageViewModel.setBitmapCache(editImageViewModel.bitmapToFilter.value)
                        }
                        collageViewModel.showViewHeader.call(true)
                    }

                    override fun onFilterTypeSelected(filterType: FilterTypeItem) {
                        Timber.d("Filter type selected: ${filterType.type}")
                        val filters: List<Filter> = when (filterType.type) {
                            FilterType.BAndW -> {
                                FirebaseEventUtils.logEventTracking(context, "filter_bw")
                                editImageViewModel.filterBAW
                            }

                            FilterType.Cinematic -> {
                                FirebaseEventUtils.logEventTracking(context, "filter_cinematic")
                                editImageViewModel.filterCinematic
                            }

                            FilterType.Landscape -> {
                                FirebaseEventUtils.logEventTracking(context, "filter_landscape")
                                editImageViewModel.filterLandscape
                            }

                            FilterType.LifeStyle -> {
                                FirebaseEventUtils.logEventTracking(context, "filter_lifestyle")
                                editImageViewModel.filterLifeStyle
                            }

                            FilterType.Moody -> {
                                FirebaseEventUtils.logEventTracking(context, "filter_moody")
                                editImageViewModel.filterMoody
                            }

                            FilterType.Nature -> {
                                FirebaseEventUtils.logEventTracking(context, "filter_nature")
                                editImageViewModel.filterNature
                            }

                            FilterType.Portrait -> {
                                FirebaseEventUtils.logEventTracking(context, "filter_portrait")
                                editImageViewModel.filterPortrait
                            }
                        }
                        filterTypeView?.setFilters(filters)

                        val currentSelectedFilter = editImageViewModel.getCurrentSelectedFilter()
                        if (currentSelectedFilter != null && filters.any { it.id == currentSelectedFilter.id }) {
                            binding?.viewRoot?.postDelayed({
                                filterTypeView?.setCurrentSelectedFilter(
                                    currentSelectedFilter,
                                    shouldScroll = true
                                )
                            }, 100)
                        }
                    }

                    override fun onFilterSelected(filter: Filter) {
                        editImageViewModel.setSelectedFilter(filter)
                        applySelectedFilter(filter)
                    }
                })
            }
            binding?.viewRoot?.addView(filterTypeView)
        }
        binding?.viewHeader?.root?.invisible()
    }

    private fun applySelectedFilter(filter: Filter) {
        val current = editImageViewModel.bitmapCache.value
        if (current == null) {
            return
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Apply filter on background thread
                val result = Trickle.applyCubeLut(
                    current, filter.lutPath, 1f
                )

                // Update UI on main thread
                launch(Dispatchers.Main) {
                    if (::framePhotoLayout.isInitialized) {
                        framePhotoLayout.updateSelectedImageWithFilter(result)
                        editImageViewModel.setBitmapToFilter(result)
                    }
                }
            } catch (e: Exception) {
                FirebaseEventUtils.recordException(e)
                launch(Dispatchers.Main) {
                    // Fallback to original image on error
                    if (::framePhotoLayout.isInitialized) {
                        framePhotoLayout.updateSelectedImageWithFilter(current)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        filterTypeView = null
        adsHomeViewModel.createInterCollage.destroy()
        framePhotoLayout.destroy()
        binding?.root?.viewTreeObserver?.removeOnGlobalLayoutListener(onGlobalListener)
        onGlobalListener = null
        handlerBindView = null
        super.onDestroyView()
        collageViewModel.clearOnDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        collageViewModel.clearAllData()
    }

    companion object {
        const val COLLAGE_NUMBER_LIMITED = 1
    }
}
