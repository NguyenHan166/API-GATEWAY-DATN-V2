package com.asoft.artsal.photo.ui.ai.fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.FragmentGenerateStoryComicBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.data.model.ComicResponse
import com.asoft.artsal.photo.data.model.StoryComicResponse
import com.asoft.artsal.photo.data.styleComicMultiple
import com.asoft.artsal.photo.data.styleComicSingle
import com.asoft.artsal.photo.extensions.initRecyclerViewAdapter
import com.asoft.artsal.photo.extensions.navigateToWithAnim
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.popBackStack
import com.asoft.artsal.photo.extensions.toast
import com.asoft.artsal.photo.ui.ai.adapter.StoryComicResultAdapter
import com.asoft.artsal.photo.ui.ai.adapter.StyleAdapter
import com.asoft.artsal.photo.ui.ai.adapter.StyleItem
import com.asoft.artsal.photo.ui.ai.adapter.StyleStoryAdapter
import com.asoft.artsal.photo.ui.ai.viewmodel.ImageViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GenerateStoryComicFragment : BaseFragment<FragmentGenerateStoryComicBinding>() {
    override val isLightStatusBar: Boolean
        get() = false

    private val viewModel: ImageViewModel by activityViewModels()

    private var selectedPages: Int = 2
    private var selectedPanels: Int = 4
    private var selectedStyleSelector: String? = null
    private var selectedQualitySelector: String = "Standard v3.1"

    private var resultAdapter: StoryComicResultAdapter? = null
    private val styleAdapter by lazy {
        StyleStoryAdapter { style ->
            selectedStyleSelector = style.styleValue
        }
    }

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentGenerateStoryComicBinding {
        return FragmentGenerateStoryComicBinding.inflate(inflater, container, false)
    }

    override fun initListener() {
        binding?.apply {
            btnBack.onClick {
                popBackStack()
            }
            btnGenerate.onClick {
                val prompt = etPrompt.text.toString().trim()
                val minLength = if (switchConfiguration.isChecked) 8 else 5
                if (prompt.length < minLength) {
                    val message = if (switchConfiguration.isChecked) {
                        "Cốt truyện phải có ít nhất 8 ký tự"
                    } else {
                        "Cốt truyện phải có ít nhất 5 ký tự"
                    }
                    requireContext().toast(message)
                    return@onClick
                }

                // Check switch state: bật = generateStoryComic, tắt = generateComic
                if (switchConfiguration.isChecked) {
                    generateStoryComic(prompt)
                } else {
                    generateComic(prompt)
                }
            }

            // Pages toggle buttons
            btnPages2.onClick {
                selectPages(2)
            }

            btnPages3.onClick {
                selectPages(3)
            }

            // Panels toggle buttons
            btnPanels3.onClick {
                selectPanels(3)
            }

            btnPanels4.onClick {
                selectPanels(4)
            }

            // Configuration switch
            switchConfiguration.setOnCheckedChangeListener { _, isChecked ->
                viewSetupPage.visibility = if (isChecked) View.VISIBLE else View.GONE
                if (isChecked) {
                    submitList(styleComicMultiple)
                } else {
                    submitList(styleComicSingle)
                }
            }


            // Character count listener
            etPrompt.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    val count = s?.length ?: 0
                    tvCharCount.text = "$count/500"
                }
            })
        }
    }

    override fun setupUi() {
        setupControls()
        setupStyleRecyclerView()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupControls() {
        binding?.apply {
            // Set default selected states
            btnPages2.isSelected = true
            btnPanels4.isSelected = true

            // Set default switch state and visibility
            switchConfiguration.isChecked = true
            viewSetupPage.visibility = View.VISIBLE

            // Setup quality selector spinner
            val qualitySelectors = listOf(
                "Standard v3.1",
                "Standard v3.0",
                "Light v3.1",
                "Heavy v3.1"
            )
            val qualityAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                qualitySelectors
            ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            spinnerQualitySelector.adapter = qualityAdapter
            spinnerQualitySelector.setSelection(0) // default Standard v3.1
            selectedQualitySelector = qualitySelectors[0]
            spinnerQualitySelector.onItemSelectedListener =
                object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: android.widget.AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        selectedQualitySelector = qualitySelectors[position]
                    }

                    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                }
        }
    }

    private fun setupStyleRecyclerView() {
        binding?.rvStyle?.initRecyclerViewAdapter(
            yourAdapter = styleAdapter,
            yourLayoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            ),
            fixedSize = false
        )
        submitList(styleComicMultiple)
    }

    private fun submitList(list: List<StyleItem>) {
        styleAdapter.submitList(list)
        styleAdapter.selectStyle(0)
        selectedStyleSelector = list[0].styleValue
    }


    private fun setupRecyclerView() {
        resultAdapter = StoryComicResultAdapter(
            onItemClick = { page ->
                // Handle item click if needed
                requireContext().toast("Clicked page ${page.pageIndex + 1}")
            }
        )
    }

    private fun selectPages(pages: Int) {
        selectedPages = pages
        binding?.apply {
            btnPages2.isSelected = pages == 2
            btnPages3.isSelected = pages == 3
        }
    }

    private fun selectPanels(panels: Int) {
        selectedPanels = panels
        binding?.apply {
            btnPanels3.isSelected = panels == 3
            btnPanels4.isSelected = panels == 4
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            showLoading(isLoading)
            binding?.btnGenerate?.isEnabled = !isLoading
        }
        viewModel.onError.observe(viewLifecycleOwner) { throwable ->
            handleException(throwable)
            requireContext().toast("Lỗi: ${throwable.message}")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    viewModel.storyComicResult.collect { result ->
                        result?.let { displayResult(it) }
                    }
                }
                launch {
                    viewModel.storyComicError.collect { error ->
                        error?.let { requireContext().toast("Lỗi tạo truyện tranh: $it") }
                    }
                }
                launch {
                    viewModel.comicResult.collect { result ->
                        result?.let { displayComicResult(it) }
                    }
                }
                launch {
                    viewModel.comicError.collect { error ->
                        error?.let { requireContext().toast("Lỗi tạo comic: $it") }
                    }
                }
            }
        }
    }

    private fun generateStoryComic(prompt: String) {
        viewModel.generateStoryComic(
            prompt = prompt,
            pages = selectedPages,
            panelsPerPage = selectedPanels,
            styleSelector = selectedStyleSelector,
            qualitySelector = selectedQualitySelector
        )
    }

    private fun generateComic(prompt: String) {
        // Map style selector to comic style format
        val comicStyle = mapStyleToComicStyle(selectedStyleSelector)

        viewModel.generateComic(
            prompt = prompt,
            pages = selectedPages,
            panelsPerPage = selectedPanels,
            style = comicStyle
        )
    }

    /**
     * Map style selector from StoryComic format to Comic format
     * StoryComic: "Anime", "Cinematic", "Realistic", etc.
     * Comic: "anime", "manga", "webtoon"
     */
    private fun mapStyleToComicStyle(styleSelector: String?): String? {
        return when (styleSelector?.lowercase()) {
            "anime" -> "anime"
            "manga" -> "manga"
            "webtoon" -> "webtoon"
            "cinematic" -> "cinematic"
            "neonpunk" -> "neonpunk"
            "3d model" -> "3d model"
            "photographic" -> "photographic"
            "digital art" -> "digital art"
            "pixel art" -> "pixel art"
            "fantasy art" -> "fantasy art"
            else -> "anime"
        }
    }

    private fun displayResult(response: StoryComicResponse) {
        navigateToWithAnim(
            R.id.action_generate_story_comic_fragment_to_result_renerate_fragment
        )
    }

    private fun displayComicResult(response: ComicResponse) {
        navigateToWithAnim(
            R.id.action_generate_story_comic_fragment_to_result_renerate_fragment
        )
    }


    override fun renderUi() {
    }
}


