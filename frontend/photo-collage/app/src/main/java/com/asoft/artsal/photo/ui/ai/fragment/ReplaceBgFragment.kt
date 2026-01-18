package com.asoft.artsal.photo.ui.ai.fragment

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.FragmentReplaceBgBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.extensions.collectIn
import com.asoft.artsal.photo.extensions.loadPhotoUri
import com.asoft.artsal.photo.extensions.navigateToWithAnim
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.popBackStack
import com.asoft.artsal.photo.extensions.toast
import com.asoft.artsal.photo.ui.ai.viewmodel.ImageViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReplaceBgFragment : BaseFragment<FragmentReplaceBgBinding>() {
    override val isLightStatusBar: Boolean
        get() = false

    private val viewModel: ImageViewModel by activityViewModels<ImageViewModel>()

    private var fgUri: Uri? = null
    private var bgUri: Uri? = null
    private var isReplaceMode: Boolean = true

    private var isShadow: Boolean = false

    // Map to store preview URIs for background images
    private val bgPreviewUris = mutableMapOf<Int, Uri>()

    private val fgPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                fgUri = it
                binding?.apply {
                    // Show the image
                    imgView.visibility = View.VISIBLE
                    imgView.loadPhotoUri(it)
                    imageContainer.setBackgroundColor(Color.TRANSPARENT)
                    // Hide upload placeholder elements
                    btnSelectImage.visibility = View.GONE
                    tvUploadText.visibility = View.GONE
                    tvUploadHint.visibility = View.GONE
                    binding?.btnProcess?.alpha = 1.0f

                }
                clearResults()
            }
        }

    private val bgPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                bgUri = it
                // Store in map and display in imgBackgroundFg
                bgPreviewUris[R.id.imgBackgroundFg] = it
                binding?.imgBackgroundFg?.apply {
                    loadPhotoUri(it)
                    visibility = View.VISIBLE
                    isSelected = false
                }
                clearResults()
            }
        }

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentReplaceBgBinding {
        return FragmentReplaceBgBinding.inflate(inflater, container, false)
    }

    override fun initListener() {
        binding?.apply {
            // Chọn ảnh gốc (foreground)
            imageContainer.setOnClickListener {
                fgPicker.launch("image/*")
            }
            btnBack.onClick {
                popBackStack()
            }

            // Nút upload background mới
            btnUploadBg.setOnClickListener {
                bgPicker.launch("image/*")
            }

            // Click listeners for background preview images
            imgBackgroundFg.setOnClickListener {
                onBgPreviewClicked(it)
            }

            imgBgPreview1.setOnClickListener {
                onBgPreviewClicked(it)
            }

            imgBgPreview2.setOnClickListener {
                onBgPreviewClicked(it)
            }

            imgBgPreview3.setOnClickListener {
                onBgPreviewClicked(it)
            }

            btnRemoveMode.setOnClickListener {
                if (!isReplaceMode) return@setOnClickListener
                isReplaceMode = false
                updateModeUI()
            }

            btnReplaceMode.setOnClickListener {
                if (isReplaceMode) return@setOnClickListener
                isReplaceMode = true
                updateModeUI()
            }

            btnProcess.setOnClickListener {
                processAction()
            }

            sliderFeather.addOnChangeListener { _, value, _ ->
                tvFeatherValue.text = "${value.toInt()}%"
            }
            iconShadow.setOnClickListener {
                isShadow = !isShadow
                if (isShadow) {
                    iconShadow.setImageResource(R.drawable.ic_shadow_selected)
                } else {
                    iconShadow.setImageResource(R.drawable.ic_shadow_un_select)
                }
            }
        }
    }


    private fun updateModeUI() {
        binding?.apply {
            btnRemoveMode.isSelected = !isReplaceMode
            btnReplaceMode.isSelected = isReplaceMode
            bgImageSection.visibility =
                if (isReplaceMode) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun onBgPreviewClicked(view: View) {
        // Clear selection from all previews
        binding?.apply {
            imgBackgroundFg.isSelected = false
            imgBgPreview1.isSelected = false
            imgBgPreview2.isSelected = false
            imgBgPreview3.isSelected = false
        }

        // Set selected state for clicked view
        view.isSelected = true

        // Get URI from map or convert drawable resource to URI
        bgUri = when (view.id) {
            R.id.imgBackgroundFg -> {
                bgPreviewUris[view.id]
            }
            R.id.imgBgPreview1 -> {
                // Get URI from drawable resource
                getUriFromDrawableResource(R.drawable.img_bg_replace1)
            }
            R.id.imgBgPreview2 -> {
                getUriFromDrawableResource(R.drawable.img_bg_replace2)
            }
            R.id.imgBgPreview3 -> {
                getUriFromDrawableResource(R.drawable.img_bg_replace3)
            }
            else -> null
        }
    }

    private fun getUriFromDrawableResource(drawableResId: Int): Uri {
        return Uri.parse("android.resource://${requireContext().packageName}/$drawableResId")
    }

    private fun processAction() {
        if (fgUri == null) {
            requireContext().toast("Vui lòng chọn ảnh gốc")
            return
        }

        if (isReplaceMode && bgUri == null) {
            requireContext().toast("Vui lòng chọn ảnh nền mới khi dùng chế độ Thay Thế Nền")
            return
        }

        val mode = if (isReplaceMode) "replace" else "remove"

        viewModel.replaceBackground(
            fgUri = fgUri!!,
            bgUri = bgUri,
            mode = mode,
            fit = "cover",
            position = "centre",
            featherPx = binding?.sliderFeather?.value?.toInt() ?: 10,
            shadow = "1",
            signTtl = 3600
        )
    }

    override fun setupUi() {
        updateModeUI() // Khởi tạo UI mode mặc định
        setupBgPreviewSelectors()
        observeViewModel()
    }

    private fun setupBgPreviewSelectors() {
        binding?.apply {
            // Apply selector background to all preview images
            imgBackgroundFg.setBackgroundResource(R.drawable.bg_style_item)
            imgBgPreview1.setBackgroundResource(R.drawable.bg_style_item)
            imgBgPreview2.setBackgroundResource(R.drawable.bg_style_item)
            imgBgPreview3.setBackgroundResource(R.drawable.bg_style_item)
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            showLoading(isLoading)
            binding?.btnProcess?.isEnabled = !isLoading
        }

        viewModel.replaceBgResult
            // Chỉ emit khi giá trị thay đổi
            .collectIn(this, action = { response ->
                binding?.apply {
                    response?.data?.url?.let { imageUrl ->
                        viewModel.imageUrlToDown = imageUrl
                        viewModel.clearReplaceBgResult()
                        navigateToWithAnim(R.id.action_replace_bg_fragment_to_result_gen_fragment)
                    }
                }
            })

        viewModel.onError.observe(viewLifecycleOwner) { throwable ->
            handleException(throwable)
            requireContext().toast("Lỗi: ${throwable.message}")
        }

        // Nếu bạn vẫn muốn hiển thị kết quả ở đâu đó (có thể chuyển sang activity mới hoặc preview)
        // Hiện tại layout không có imgResult → bạn có thể navigate sang màn hình kết quả
    }

    private fun clearResults() {
        // Nếu có phần preview kết quả trong fragment này thì clear
        // Hiện tại không có → bỏ qua hoặc xử lý khi navigate
    }

    override fun renderUi() {
        // Không cần nếu đã xử lý trong setupUi và listener
    }
}

