package com.asoft.artsal.photo.ui.ai.fragment

import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.FragmentStyleBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.data.model.StyleTransferResponse
import com.asoft.artsal.photo.extensions.initRecyclerViewAdapter
import com.asoft.artsal.photo.extensions.loadPhotoUri
import com.asoft.artsal.photo.extensions.navigateToWithAnim
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.popBackStack
import com.asoft.artsal.photo.extensions.toast
import com.asoft.artsal.photo.extensions.visible
import com.asoft.artsal.photo.ui.ai.adapter.StyleAdapter
import com.asoft.artsal.photo.ui.ai.adapter.StyleItem
import com.asoft.artsal.photo.ui.ai.viewmodel.ImageViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StyleFragment : BaseFragment<FragmentStyleBinding>() {
    override val isInsets: Boolean
        get() = true

    override val isLightStatusBar: Boolean
        get() = false

    private val viewModel: ImageViewModel by activityViewModels<ImageViewModel>()

    private var selectedImageUri: Uri? = null
    private var selectedStyle: String = "anime"
    private var keyboardVisible = false
    private val handler = Handler(Looper.getMainLooper())

    private val styleAdapter by lazy {
        StyleAdapter { style ->
            selectedStyle = style.styleValue?.lowercase() ?: style.id
        }
    }

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding?.apply {
                imgView.visible()
                imgView.loadPhotoUri(it)
                btnSelectImage.visibility = View.GONE
                tvUploadText.visibility = View.GONE
                tvUploadHint.visibility = View.GONE
                imageContainer.setBackgroundColor(Color.TRANSPARENT)
                btnApplyStyle.alpha = 1.0f
            }
            clearResults()
        }
    }

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentStyleBinding {
        return FragmentStyleBinding.inflate(inflater, container, false)
    }

    override fun initListener() {
        binding?.apply {
            btnBack.onClick {
                popBackStack()
            }

            btnSelectImage.onClick {
                imagePicker.launch("image/*")
            }

            btnApplyStyle.onClick {
                if (selectedImageUri == null) {
                    requireContext().toast("Vui lòng chọn ảnh trước")
                    return@onClick
                }
                applyStyle()
            }

            // Scroll to EditText when it gets focus (keyboard opens)
            etExtra.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    scrollToEditText(view)
                }
            }

            setupKeyboardListener()
        }
    }

    override fun setupUi() {
        setupStyleRecyclerView()
        observeViewModel()
    }


    private fun setupKeyboardListener() {
        binding?.root?.viewTreeObserver?.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val rootView = binding?.root ?: return
                    val rect = android.graphics.Rect()
                    rootView.getWindowVisibleDisplayFrame(rect)
                    val screenHeight = rootView.height
                    val keypadHeight = screenHeight - rect.bottom

                    val wasKeyboardVisible = keyboardVisible
                    keyboardVisible = keypadHeight > screenHeight * 0.15 // Threshold for keyboard

                    // Add padding to LinearLayout when keyboard is visible
                    binding?.nestedScrollView?.getChildAt(0)?.let { contentView ->
                        if (contentView is ViewGroup) {
                            val paddingBottom = if (keyboardVisible) {
                                keypadHeight.coerceAtMost(600) // Max 400dp padding
                            } else {
                                0
                            }
                            contentView.setPadding(
                                contentView.paddingLeft,
                                contentView.paddingTop,
                                contentView.paddingRight,
                                paddingBottom
                            )
                        }
                    }

                    // If keyboard just became visible and EditText has focus, scroll to it
                    if (keyboardVisible && !wasKeyboardVisible) {
                        binding?.etExtra?.let { editText ->
                            if (editText.hasFocus()) {
                                handler.postDelayed({
                                    scrollToEditText(editText)
                                }, 150)
                            }
                        }
                    }
                }
            }
        )
    }

    private fun scrollToEditText(editText: View) {
        binding?.nestedScrollView?.let { scrollView ->
            handler.postDelayed({
                val rect = android.graphics.Rect()
                editText.getHitRect(rect)
                // Add padding to ensure EditText is visible above keyboard
                rect.bottom += 200 // Add extra space for keyboard
                scrollView.requestRectangleOnScreen(rect, true)
            }, 100)

            // Also try smooth scroll as backup
            handler.postDelayed({
                // Get EditText position relative to scroll view content
                val scrollBounds = android.graphics.Rect()
                scrollView.getHitRect(scrollBounds)

                val editTextBounds = android.graphics.Rect()
                editText.getGlobalVisibleRect(editTextBounds)

                val scrollViewLocation = IntArray(2)
                scrollView.getLocationOnScreen(scrollViewLocation)
                editTextBounds.offset(-scrollViewLocation[0], -scrollViewLocation[1])

                // Calculate scroll position - show EditText in upper 2/3 of visible area
                val visibleHeight = scrollView.height
                val targetY = (editTextBounds.top - visibleHeight / 3).coerceAtLeast(0)

                scrollView.smoothScrollTo(0, targetY)
            }, 200)
        }
    }

    private fun setupStyleRecyclerView() {
        // Create list of styles for style transfer
        val styles = listOf(
            StyleItem(
                id = "anime",
                name = "Anime",
                imageRes = R.drawable.img_anime,
                styleValue = "anime"
            ),
            StyleItem(
                id = "ghibli",
                name = "Ghibli",
                imageRes = R.drawable.img_ghibli,
                styleValue = "ghibli"
            ),
            StyleItem(
                id = "watercolor",
                name = "Watercolor",
                imageRes = R.drawable.img_water_color,
                styleValue = "watercolor"
            ),
            StyleItem(
                id = "sketches",
                name = "Sketches",
                imageRes = R.drawable.img_sketches,
                styleValue = "sketches"
            ),
            StyleItem(
                id = "cartoon",
                name = "Cartoon",
                imageRes = R.drawable.img_cartoon,
                styleValue = "cartoon"
            )
        )

        styleAdapter.submitList(styles)

        // Setup RecyclerView
        binding?.rvStyle?.initRecyclerViewAdapter(
            yourAdapter = styleAdapter,
            yourLayoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            ),
            fixedSize = false
        )

        // Select first item by default
        styleAdapter.selectStyle(0)
        selectedStyle = styles[0].styleValue ?: styles[0].id
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            showLoading(isLoading)
            binding?.btnApplyStyle?.isEnabled = !isLoading
        }
        viewModel.onError.observe(viewLifecycleOwner) { throwable ->
            handleException(throwable)
            requireContext().toast("Error: ${throwable.message}")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    viewModel.styleTransferResult.collect { result ->
                        result?.let { displayResult(it) }
                    }
                }
                launch {
                    viewModel.styleTransferError.collect { error ->
                        error?.let { requireContext().toast("Style error: $it") }
                    }
                }
            }
        }
    }

    private fun applyStyle() {
        val uri = selectedImageUri ?: return
        val extraText = binding?.etExtra?.text?.toString()?.trim().orEmpty()
        val extra = extraText.takeIf { it.isNotEmpty() }
        viewModel.applyStyle(
            imageUri = uri,
            style = selectedStyle,
            extra = extra
        )
    }

    private fun displayResult(response: StyleTransferResponse) {
        binding?.apply {
            response.data.url.let { url ->
                viewModel.imageUrlToDown = url
                viewModel.clearStyleTransferResult()
                navigateToWithAnim(R.id.action_style_fragment_to_result_gen_fragment)
            }
        }
    }

    private fun clearResults() {
        viewModel.clearStyleTransferResult()
    }

    override fun renderUi() {
    }
}