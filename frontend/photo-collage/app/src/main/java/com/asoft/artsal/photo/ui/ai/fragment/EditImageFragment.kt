package com.asoft.artsal.photo.ui.ai.fragment

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.artsal.photo.editor.collage.maker.databinding.FragmentEditImageBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.data.model.BaseData
import com.asoft.artsal.photo.data.model.BaseResponse
import com.asoft.artsal.photo.extensions.loadPhotoUri
import com.asoft.artsal.photo.extensions.loadImageFromUrlWithLoading
import com.asoft.artsal.photo.extensions.toast
import com.asoft.artsal.photo.ui.ai.viewmodel.ImageViewModel
import com.google.gson.GsonBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class EditImageFragment : BaseFragment<FragmentEditImageBinding>() {

    private val viewModel: ImageViewModel by viewModels()

    // ==================== UPSCALE STATE ====================
    private var selectedImageUri: Uri? = null
    private var selectedScale: Int? = null
    private var selectedVersion: String? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding?.imgView?.loadPhotoUri(it)
            clearResults()
        }
    }

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentEditImageBinding {
        return FragmentEditImageBinding.inflate(inflater, container, false)
    }

    override fun initListener() {
        binding?.apply {
            // Image selection
            btnSelectImage.setOnClickListener {
                imagePickerLauncher.launch("image/*")
            }

            // ==================== UPSCALE BUTTON ====================
            btnUpscale.setOnClickListener {
                if (selectedImageUri == null) {
                    requireContext().toast("Please select an image first")
                    return@setOnClickListener
                }
                upscaleImage()
            }
        }
    }

    override fun setupUi() {
        setupUpscaleControls()
        observeViewModel()
    }

    // ==================== UPSCALE SETUP ====================
    private fun setupUpscaleControls() {
        binding?.apply {
            // Scale spinner
            val scaleOptions = listOf("Default (2)", "1", "2", "4")
            val scaleAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                scaleOptions
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            spinnerScale.adapter = scaleAdapter
            spinnerScale.setSelection(0) // Default to "Default (2)"

            spinnerScale.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedScale = when (position) {
                        0 -> null // Default
                        1 -> 1
                        2 -> 2
                        3 -> 4
                        else -> null
                    }
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

            // Version spinner
            val versionOptions = listOf("Default (v1.4)", "v1.3", "v1.4")
            val versionAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                versionOptions
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            spinnerVersion.adapter = versionAdapter
            spinnerVersion.setSelection(0) // Default to "Default (v1.4)"

            spinnerVersion.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedVersion = when (position) {
                        0 -> null // Default
                        1 -> "v1.3"
                        2 -> "v1.4"
                        else -> null
                    }
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        }
    }


    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            showLoading(isLoading)
            binding?.btnUpscale?.isEnabled = !isLoading
        }

        viewModel.onError.observe(viewLifecycleOwner) { throwable ->
            handleException(throwable)
            requireContext().toast("Error: ${throwable.message}")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                // ==================== UPSCALE OBSERVERS ====================
                launch {
                    viewModel.upscaleResult.collect { result ->
                        result?.let { response ->
                            displayUpscaleResult(response)
                        }
                    }
                }

                launch {
                    viewModel.upscaleError.collect { error ->
                        error?.let {
                            requireContext().toast("Upscale Error: $it")
                            Timber.e("Upscale error: $it")
                        }
                    }
                }
            }
        }
    }

    // ==================== UPSCALE FUNCTIONS ====================
    private fun upscaleImage() {
        selectedImageUri?.let { uri ->
            viewModel.upscaleImage(uri, selectedScale, selectedVersion)
        }
    }

    private fun displayUpscaleResult(response: BaseResponse<BaseData>) {
        binding?.apply {
            val resultText = buildString {
                appendLine("=== UPSCALE RESULT ===")
                appendLine("Status: ${response.status}")
                appendLine("Request ID: ${response.requestId}")
                response.data?.let { data ->
                    appendLine("\nData:")
                    appendLine("  Key: ${data.key}")
                    appendLine("  URL: ${data.url}")
                    appendLine("  Presigned URL: ${data.presignedUrl}")
                    appendLine("  Expires In: ${data.expiresIn}s")
                }
                response.meta?.let { meta ->
                    appendLine("\nMeta:")
                    meta.model?.let { appendLine("  Model: $it") }
                    meta.version?.let { appendLine("  Version: $it") }
                    meta.scale?.let { appendLine("  Scale: $it") }
                    meta.inputSize?.let {
                        appendLine("  Input Size: ${it.width}x${it.height}")
                    }
                    meta.outputSize?.let {
                        appendLine("  Output Size: ${it.width}x${it.height}")
                    }
                }
            }

            tvUpscaleResult.text = resultText
            tvUpscaleResult.visibility = View.VISIBLE

            // Display result image
            response.data?.url?.let { imageUrl ->
                imgUpscaleResult.visibility = View.VISIBLE
                imgUpscaleResult.loadImageFromUrlWithLoading(imageUrl)
            } ?: run {
                imgUpscaleResult.visibility = View.GONE
            }
        }
    }

    // ==================== HELPER FUNCTIONS ====================
    private fun clearResults() {
        binding?.apply {
            tvUpscaleResult.visibility = View.GONE
            imgUpscaleResult.visibility = View.GONE
        }
        viewModel.clearUpscaleResult()
    }

    override fun renderUi() {
        // Both sections are always visible in the layout
    }
}