package com.asoft.artsal.photo.ui.ai.fragment

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.FragmentRelightBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.data.model.BaseResponse
import com.asoft.artsal.photo.data.model.RelightData
import com.asoft.artsal.photo.extensions.downloadAndSaveImageFromUrl
import com.asoft.artsal.photo.extensions.loadPhotoUri
import com.asoft.artsal.photo.extensions.loadImageFromUrlWithLoading
import com.asoft.artsal.photo.extensions.navigateToWithAnim
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.popBackStack
import com.asoft.artsal.photo.extensions.toast
import com.asoft.artsal.photo.extensions.visible
import com.asoft.artsal.photo.ui.ai.viewmodel.ImageViewModel
import com.asoft.artsal.photo.utils.Const
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class RelightFragment : BaseFragment<FragmentRelightBinding>() {
    override val isLightStatusBar: Boolean
        get() = false

    private val viewModel: ImageViewModel by activityViewModels<ImageViewModel>()

    // ==================== RELIGHT PORTRAIT STATE ====================
    private var selectedImageUri: Uri? = null
    private var selectedDirection: String? = null // "top_left", "top", "top_right", "left", "front", "right"
    private var intensity: Float = 88f // 0-100
    private var steps: Float = 25f // 1-50
    private var cfgScale: Float = 7.5f // 1-20


    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding?.apply {
                // Show the image
                imgView.visibility = View.VISIBLE
                imgView.loadPhotoUri(it)
                imageContainer.setBackgroundColor(Color.TRANSPARENT)
                // Hide upload placeholder elements
                btnSelectImage.visibility = View.GONE
                tvUploadText.visibility = View.GONE
                tvUploadHint.visibility = View.GONE
                btnRelight.alpha = 1.0f
            }
        }
    }

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentRelightBinding {
        return FragmentRelightBinding.inflate(inflater, container, false)
    }

    override fun initListener() {
        binding?.apply {
            // Back button
            btnBack.onClick {
                popBackStack()
            }

            // Image selection
            imageContainer.onClick {
                imagePickerLauncher.launch("image/*")
            }

            btnTop.onClick {
                selectDirection("top", btnTop)
            }


            btnLeft.onClick {
                selectDirection("left", btnLeft)
            }



            btnRight.onClick {
                selectDirection("right", btnRight)
            }

            // Reset button
            btnReset.onClick {
                resetDirection()
            }

            // Sliders
            sliderOutputQuality.addOnChangeListener { _, value, _ ->
                intensity = value
                tvOutputQualityValue.text = "${value.toInt()}%"
            }

            sliderSteps.addOnChangeListener { _, value, _ ->
                steps = value
                tvStepsValue.text = value.toInt().toString()
            }

            sliderCfgScale.addOnChangeListener { _, value, _ ->
                cfgScale = value
                tvCfgScaleValue.text = String.format("%.1f", value)
            }

            // Generate Relight button
            btnRelight.onClick {
                if (selectedImageUri == null) {
                    requireContext().toast("Vui lòng chọn ảnh trước")
                    return@onClick
                }
                val prompt = etPrompt.text.toString().trim()
                if (prompt.isEmpty()) {
                    requireContext().toast("Vui lòng nhập mô tả ánh sáng")
                    return@onClick
                }
                relightPortrait(prompt)
            }
        }
    }


    override fun setupUi() {
        setupRelightControls()
        observeViewModel()
    }

    // ==================== RELIGHT PORTRAIT SETUP ====================
    private fun setupRelightControls() {
        binding?.apply {
            // Set default prompt
            etPrompt.setText("studio soft light, flattering portrait lighting")

            // Set default values for sliders
            sliderOutputQuality.value = intensity
            sliderSteps.value = steps
            sliderCfgScale.value = cfgScale

            // Update text values
            tvOutputQualityValue.text = "${intensity.toInt()}%"
            tvStepsValue.text = steps.toInt().toString()
            tvCfgScaleValue.text = String.format("%.1f", cfgScale)

            // Default direction: Left (as shown in image)
            selectDirection("left", btnLeft)
        }
    }

    private fun selectDirection(direction: String, selectedButton: View) {
        selectedDirection = direction

        // Deselect all direction buttons
        binding?.apply {
            btnTop.isSelected = false
            btnLeft.isSelected = false
            btnRight.isSelected = false
        }

        // Select the clicked button
        selectedButton.isSelected = true
    }

    private fun resetDirection() {
        selectedDirection = null
        binding?.apply {
            btnTop.isSelected = false
            btnLeft.isSelected = false
            btnRight.isSelected = false
        }
    }

    private fun mapDirectionToLightSource(direction: String?): String {
        return when (direction) {
            "top_left", "top", "top_right" -> "Top Light"
            "left" -> "Left Light"
            "right" -> "Right Light"
            "front", null -> "None"
            else -> "None"
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            showLoading(isLoading)
            binding?.btnRelight?.isEnabled = !isLoading
        }

        viewModel.onError.observe(viewLifecycleOwner) { throwable ->
            handleException(throwable)
            requireContext().toast("Lỗi: ${throwable.message}")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                // ==================== RELIGHT PORTRAIT OBSERVERS ====================
                launch {
                    viewModel.relightResult.collect { result ->
                        result?.let { response ->
                            displayRelightResult(response)
                        }
                    }
                }

                launch {
                    viewModel.relightError.collect { error ->
                        error?.let {
                            requireContext().toast("Lỗi relight: $it")
                            Timber.e("Relight error: $it")
                        }
                    }
                }
            }
        }
    }

    // ==================== RELIGHT PORTRAIT FUNCTIONS ====================
    private fun relightPortrait(prompt: String) {
        selectedImageUri?.let { uri ->
            val lightSource = mapDirectionToLightSource(selectedDirection)

            Timber.d("Relight request - prompt: '$prompt', direction: '$selectedDirection', lightSource: '$lightSource'")
            Timber.d("Relight params - intensity: $intensity, steps: $steps, cfgScale: $cfgScale")


            viewModel.relightPortrait(
                imageUri = uri,
                prompt = prompt,
                lightSource = lightSource
            )
        }
    }

    private fun displayRelightResult(response: BaseResponse<RelightData>) {
        binding?.apply {
            response.data?.outputs?.firstOrNull()?.url?.let { imageUrl ->
                viewModel.imageUrlToDown = imageUrl
                viewModel.clearRelightResult()
                navigateToWithAnim(R.id.action_relight_fragment_to_result_gen_fragment)
            }
        }
    }

    override fun renderUi() {
        // Initial UI state
    }
}