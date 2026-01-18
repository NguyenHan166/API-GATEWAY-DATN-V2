package com.asoft.artsal.photo.ui.ai.fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.artsal.photo.editor.collage.maker.databinding.FragmentGenerateComicBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.data.model.ComicResponse
import com.asoft.artsal.photo.extensions.toast
import com.asoft.artsal.photo.ui.ai.viewmodel.ImageViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GenerateComicFragment : BaseFragment<FragmentGenerateComicBinding>() {

    private val viewModel: ImageViewModel by viewModels()

    private var selectedPages: Int? = null
    private var selectedPanels: Int? = null
    private var selectedStyle: String? = null

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentGenerateComicBinding {
        return FragmentGenerateComicBinding.inflate(inflater, container, false)
    }

    override fun initListener() {
        binding?.apply {
            btnGenerate.setOnClickListener {
                val prompt = etPrompt.text.toString().trim()
                if (prompt.length < 5) {
                    requireContext().toast("Prompt must be at least 5 characters")
                    return@setOnClickListener
                }
                generateComic(prompt)
            }
        }
    }

    override fun setupUi() {
        setupControls()
        observeViewModel()
    }

    private fun setupControls() {
        binding?.apply {
            // pages 1-3
            val pages = listOf(1, 2, 3)
            val pagesAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                pages
            ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            spinnerPages.adapter = pagesAdapter
            spinnerPages.setSelection(1) // default 2
            selectedPages = pages[1]
            spinnerPages.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedPages = pages[position]
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

            // panels per page 3-9
            val panels = (3..9).toList()
            val panelsAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                panels
            ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            spinnerPanels.adapter = panelsAdapter
            spinnerPanels.setSelection(1) // default 4
            selectedPanels = panels[1]
            spinnerPanels.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedPanels = panels[position]
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

            // style choices
            val styles = listOf("anime", "manga", "webtoon")
            val styleAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                styles
            ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            spinnerStyle.adapter = styleAdapter
            spinnerStyle.setSelection(0)
            selectedStyle = styles[0]
            spinnerStyle.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedStyle = styles[position]
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            showLoading(isLoading)
            binding?.btnGenerate?.isEnabled = !isLoading
        }
        viewModel.onError.observe(viewLifecycleOwner) { throwable ->
            handleException(throwable)
            requireContext().toast("Error: ${throwable.message}")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    viewModel.comicResult.collect { result ->
                        result?.let { displayResult(it) }
                    }
                }
                launch {
                    viewModel.comicError.collect { error ->
                        error?.let { requireContext().toast("Comic error: $it") }
                    }
                }
            }
        }
    }

    private fun generateComic(prompt: String) {
        viewModel.generateComic(
            prompt = prompt,
            pages = selectedPages,
            panelsPerPage = selectedPanels,
            style = selectedStyle
        )
    }

    private fun displayResult(response: ComicResponse) {
        binding?.apply {
            val resultText = buildString {
                appendLine("=== COMIC RESULT ===")
                appendLine("Status: ${response.status}")
                appendLine("Comic ID: ${response.data.comicId}")
                appendLine("Pages: ${response.data.panels.size}")
            }
            tvResult.text = resultText
            tvResult.visibility = View.VISIBLE
        }
    }

    private fun clearResults() {
        binding?.tvResult?.visibility = View.GONE
        viewModel.clearComicResult()
    }

    override fun renderUi() {
        // handled in setupUi
    }
}



