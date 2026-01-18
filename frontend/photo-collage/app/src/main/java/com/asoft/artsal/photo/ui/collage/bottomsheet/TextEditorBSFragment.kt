package com.asoft.artsal.photo.ui.collage.bottomsheet

import android.content.DialogInterface
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.BottomSheetTextEditorBinding
import com.artsal.photo.editor.collage.maker.databinding.ItemTabAddTextBinding
import com.asoft.artsal.photo.base.BaseSafeDialogFragment
import com.asoft.artsal.photo.component.GridSpacingItemDecoration
import com.asoft.artsal.photo.data.fontTypes
import com.asoft.artsal.photo.data.model.FontType
import com.asoft.artsal.photo.extensions.collectIn
import com.asoft.artsal.photo.extensions.dpToPx
import com.asoft.artsal.photo.extensions.focusAndShowKeyboard
import com.asoft.artsal.photo.extensions.gone
import com.asoft.artsal.photo.extensions.hideKeyboard
import com.asoft.artsal.photo.extensions.initRecyclerViewAdapter
import com.asoft.artsal.photo.extensions.internal.getAdsKey
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.showKeyboard
import com.asoft.artsal.photo.extensions.visible
import com.asoft.artsal.photo.ui.ads.AdsHomeViewModel
import com.asoft.artsal.photo.ui.collage.adapter.text.ColorTypeAdapter
import com.asoft.artsal.photo.ui.collage.adapter.text.FontStyleAdapter
import com.asoft.artsal.photo.ui.collage.viewmodel.CollageViewModel
import com.asoft.artsal.photo.ui.home.viewmodel.EditImageViewModel
import com.asoft.artsal.photo.utils.AdsIdUtils
import com.asoft.artsal.photo.utils.AppUtils
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import com.asoft.artsal.photo.utils.RemoteConfigAdsConst
import com.google.android.material.tabs.TabLayout
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.minsap.ad.listener.BannerAdCallBack
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TextEditorBSFragment : BaseSafeDialogFragment<BottomSheetTextEditorBinding>() {
    override val isInsets: Boolean
        get() = false
    private val collageViewModel: CollageViewModel by activityViewModels<CollageViewModel>()
    private val editImageViewModel: EditImageViewModel by activityViewModels<EditImageViewModel>()
    private val adsHomeViewModel: AdsHomeViewModel by activityViewModels<AdsHomeViewModel>()

    @Inject
    lateinit var firebaseRemoteConfig: FirebaseRemoteConfig

    private val fontAdapter by lazy {
        FontStyleAdapter(
            ::onSelectFont
        )
    }
    private val colorAdapter by lazy {
        ColorTypeAdapter(
            ::onSelectColor
        )
    }

    interface TextEditorListener {
        fun onDone(inputText: String)
        fun onClose()
    }

    private var isKeyboardVisible = false

    private var itemTabViewBinding: ItemTabAddTextBinding? = null
    private var onGlobalListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    private var textEditorListener: TextEditorListener? = null

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): BottomSheetTextEditorBinding {
        return BottomSheetTextEditorBinding.inflate(layoutInflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }


    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
    }

    override fun getTheme(): Int {
        return R.style.KeyboardInputStyle
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun setupUi() {
        loadBannerAds()
        setupTabLayout()
        setupListener()
        setupKeyboardListener()
        observerData()
        binding?.editText?.apply {
            val colorCode = collageViewModel.textStyle.value.textColor
            val font = collageViewModel.textStyle.value.fontStyle
            setText(collageViewModel.text.value)
            setTextColor(colorCode)
            focusAndShowKeyboard()
            isFocusable = true
            isFocusableInTouchMode = true
            isEnabled = true
            requestFocus()
            setSelection(text?.length ?: 0)
            filters = arrayOf(InputFilter.LengthFilter(100))
            try {
                setTypeface(ResourcesCompat.getFont(context ?: return@apply, font.font))
            } catch (e: Exception) {
                FirebaseEventUtils.recordException(e)
            }
        }
        binding?.run {
            rvColor.initRecyclerViewAdapter(
                yourAdapter = colorAdapter,
                yourLayoutManager = LinearLayoutManager(
                    context ?: return, LinearLayoutManager.HORIZONTAL, false
                ),
                fixedSize = true,
            )
            rvFont.initRecyclerViewAdapter(
                yourAdapter = fontAdapter,
                yourLayoutManager = GridLayoutManager(context ?: return, 3),
                fixedSize = true,
            )
            rvFont.addItemDecoration(
                GridSpacingItemDecoration(
                    3, 8.dpToPx(), 8.dpToPx(), false, 0
                )
            )
        }
        colorAdapter.submitList(AppUtils.colorTypes)
        fontAdapter.submitList(fontTypes)
    }

    private fun observerData() {
        collageViewModel.textStyle.collectIn(
            this@TextEditorBSFragment
        ) { textStyle ->
            binding?.editText.apply {
                this?.setTextColor(textStyle.textColor)
                this?.setTypeface(
                    ResourcesCompat.getFont(
                        requireContext(),
                        textStyle.fontStyle.font
                    )
                )
                colorAdapter.setCurrentColor(textStyle.textColor)
                fontAdapter.setCurrentFont(textStyle.fontStyle)
            }
        }
    }

    private fun setupTabLayout() {
        if (binding?.tabLayout == null) {
            return
        }
        val tabs = getTabs()
        tabs.forEachIndexed { index, tabInfo ->
            val tab = binding?.tabLayout?.newTab()?.apply {
                customView = createCustomTabView(
                    context?.resources?.getString(tabInfo.first) ?: "", tabInfo.second
                )
            }
            binding?.tabLayout?.addTab(tab ?: return@forEachIndexed, index)
        }
        binding?.run {
            tabLayout.getTabAt(0)?.select()
        }
        val tab = binding?.tabLayout?.getTabAt(1)
        val customView = tab?.customView
        val tabText = customView?.findViewById<TextView>(R.id.tvTitleTab)
        tabText?.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.txt_nor_opacity_50)
        )
    }

    private fun setupListener() {
        binding?.run {
            btnClose.onClick {
                FirebaseEventUtils.logEventTracking(context, "text_close")
                binding?.editText?.apply {
                    hideKeyboard()
                    clearFocus()
                    isFocusable = false
                    isFocusableInTouchMode = false
                }
                showHeader()
                dismiss()
            }
            btnDone.onClick {
                FirebaseEventUtils.logEventTracking(context, "text_save")

                binding?.editText?.apply {
                    hideKeyboard()
                    clearFocus()
                    isFocusable = false
                    isFocusableInTouchMode = false
                }
                showHeader()
                textEditorListener?.onDone(
                    binding?.editText?.text?.toString() ?: return@onClick,
                )
                dismiss()
            }
        }

        binding?.tabLayout?.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    val customView = tab.customView
                    val tabText = customView?.findViewById<TextView>(R.id.tvTitleTab)
                    val tabIcon = customView?.findViewById<ImageView>(R.id.icTab)
                    ContextCompat.getColor(
                        requireContext(), R.color.txt_normal
                    ).let { it ->
                        tabText?.setTextColor(it)
                    }
                    when (tab.position) {
                        0 -> {
                            tabIcon?.setImageResource(R.drawable.ic_keyboard)
                            binding?.editText?.apply {
                                showKeyboard()
                                isFocusable = true
                                isFocusableInTouchMode = true
                                isEnabled = true
                                requestFocus()
                                setSelection(text?.length ?: 0)
                            }
                            showContent()
                        }

                        1 -> {
                            binding?.editText?.apply {
                                hideKeyboard()
                                clearFocus()
                                isFocusable = false
                                isFocusableInTouchMode = false
                            }
                            tabIcon?.setImageResource(R.drawable.ic_palette)
                            binding?.root?.postDelayed({
                                showContent(true)
                            }, 50)

                        }

                        else -> {}
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.let {
                    val customView = tab.customView
                    val tabText = customView?.findViewById<TextView>(R.id.tvTitleTab)
                    val tabIcon = customView?.findViewById<ImageView>(R.id.icTab)
                    ContextCompat.getColor(
                        requireContext(), R.color.txt_nor_opacity_50
                    ).let { it ->
                        tabText?.setTextColor(it)
                    }
                    when (tab.position) {
                        0 -> {
                            tabIcon?.setImageResource(R.drawable.ic_keyboard_unselected)
                        }

                        1 -> {
                            tabIcon?.setImageResource(R.drawable.ic_palette_unselected)
                        }

                        else -> {}
                    }
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
    }

    private fun showContent(isShow: Boolean = false) {
        binding?.run {
            if (isShow) {
                rvColor.visible()
                rvFont.visible()
                bannerAds.root.visible()
            } else {
                bannerAds.root.gone()
                rvColor.gone()
                rvFont.gone()
            }
        }
    }

    private fun createCustomTabView(title: String, icon: Int): View {
        itemTabViewBinding = ItemTabAddTextBinding.inflate(layoutInflater)
        itemTabViewBinding?.apply {
            icTab.setImageResource(icon)
            tvTitleTab.text = (title)
        }
        return itemTabViewBinding?.root ?: return View(context)
    }

    private fun getTabs() = listOf(
        Pair(R.string.keyboard, R.drawable.ic_keyboard),
        Pair(R.string.style, R.drawable.ic_palette_unselected),
    )

    fun onSelectFont(font: FontType) {
        collageViewModel.setFontStyle(font)
    }

    fun onSelectColor(color: Int) {
        collageViewModel.setTextColor(color)
    }

    private fun loadBannerAds() {
        if (firebaseRemoteConfig.getAdsKey(context, RemoteConfigAdsConst.BANNER_COLLAGE_SHOW)) {
            adsHomeViewModel.createBannerAdEditCollage.loadAndShowBannerAd(
                activity,
                AdsIdUtils.BANNER_COLLAGE,
                adName = "Collage",
                frameAd = binding?.bannerAds?.root,
                callBackResult = object : BannerAdCallBack() {
                })
        }
    }

    private fun setupKeyboardListener() {
        onGlobalListener = null
        val rootView = activity?.window?.decorView?.rootView
        onGlobalListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            rootView?.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView?.height
            val keypadHeight = screenHeight?.minus(rect.bottom)
            val wasKeyboardVisible = isKeyboardVisible
            if (keypadHeight != null) {
                isKeyboardVisible = keypadHeight > screenHeight * 0.15
            }
            if (wasKeyboardVisible && !isKeyboardVisible) {
                val position = binding?.tabLayout?.selectedTabPosition
                if (position == 0) {
                    showHeader()
                    dismiss()
                }
            }
        }
        rootView?.viewTreeObserver?.addOnGlobalLayoutListener(onGlobalListener)
    }

    private fun showHeader() {
        collageViewModel.showViewHeader.call(true)
    }

    fun setupTextListener(listener: TextEditorListener) {
        textEditorListener = listener
    }

    override fun onDestroyView() {
        binding?.root?.viewTreeObserver?.removeOnGlobalLayoutListener(onGlobalListener)
        onGlobalListener = null
        textEditorListener = null
        super.onDestroyView()
        adsHomeViewModel.createBannerAdEditCollage.destroy()
    }

    companion object {
        @JvmStatic
        fun newInstance() = TextEditorBSFragment()
    }
}
