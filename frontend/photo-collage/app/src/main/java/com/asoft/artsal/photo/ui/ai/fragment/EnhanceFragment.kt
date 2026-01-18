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
import com.artsal.photo.editor.collage.maker.databinding.FragmentEnhanceBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.data.model.BaseData
import com.asoft.artsal.photo.data.model.BaseResponse
import com.asoft.artsal.photo.extensions.loadImageFromUrlWithLoading
import com.asoft.artsal.photo.extensions.loadPhotoUri
import com.asoft.artsal.photo.extensions.toast
import com.asoft.artsal.photo.ui.ai.viewmodel.ImageViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EnhanceFragment : BaseFragment<FragmentEnhanceBinding>() {

    private val viewModel: ImageViewModel by viewModels()

    private var selectedImageUri: Uri? = null
    private var selectedScale: Int = 2

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding?.imgPreview?.loadPhotoUri(it)
            clearResults()
        }
    }

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentEnhanceBinding {
        return FragmentEnhanceBinding.inflate(inflater, container, false)
    }

    override fun initListener() {
        binding?.apply {
            btnSelectImage.setOnClickListener {
                imagePickerLauncher.launch("image/*")
            }

            btnEnhance.setOnClickListener {
                if (selectedImageUri == null) {
                    requireContext().toast("Please select an image first")
                    return@setOnClickListener
                }
                enhanceImage()
            }
        }
    }

    override fun setupUi() {
        setupControls()
        observeViewModel()
    }

    private fun setupControls() {
        binding?.apply {
            // scale options: 2 or 4
            val scaleOptions = listOf(2, 4)
            val scaleAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                scaleOptions
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            spinnerScale.adapter = scaleAdapter
            spinnerScale.setSelection(0)
            spinnerScale.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedScale = scaleOptions[position]
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

            etModel.setText("real-esrgan")
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            showLoading(isLoading)
            binding?.btnEnhance?.isEnabled = !isLoading
        }

        viewModel.onError.observe(viewLifecycleOwner) { throwable ->
            handleException(throwable)
            requireContext().toast("Error: ${throwable.message}")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    viewModel.enhanceResult.collect { result ->
                        result?.let { displayResult(it) }
                    }
                }
                launch {
                    viewModel.enhanceError.collect { error ->
                        error?.let { requireContext().toast("Enhance error: $it") }
                    }
                }
            }
        }
    }

    private fun enhanceImage() {
        val uri = selectedImageUri ?: return
        val scale = selectedScale
        val faceEnhance = binding?.switchFaceEnhance?.isChecked
        val modelText = binding?.etModel?.text?.toString()?.trim()
        val model = modelText?.takeIf { it.isNotEmpty() }

        viewModel.enhanceImage(
            imageUri = uri,
            scale = scale,
            faceEnhance = faceEnhance,
            model = model
        )
    }

    private fun displayResult(response: BaseResponse<BaseData>) {
        binding?.apply {
            val resultText = buildString {
                appendLine("=== ENHANCE RESULT ===")
                appendLine("Status: ${response.status}")
                appendLine("Request ID: ${response.requestId}")
                response.data?.url?.let { appendLine("Output: $it") }
            }
            tvResult.text = resultText
            tvResult.visibility = View.VISIBLE

            response.data?.url?.let { imageUrl ->
                imgResult.visibility = View.VISIBLE
                imgResult.loadImageFromUrlWithLoading(imageUrl)
            } ?: run {
                imgResult.visibility = View.GONE
            }
        }
    }

    private fun clearResults() {
        binding?.apply {
            tvResult.visibility = View.GONE
            imgResult.visibility = View.GONE
        }
        viewModel.clearEnhanceResult()
    }

    override fun renderUi() {
        // initial state handled in setupUi
    }
}