package com.asoft.artsal.photo.ui.ai.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.FragmentResultRenerateBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.data.model.ComicResponse
import com.asoft.artsal.photo.data.model.StoryComicPage
import com.asoft.artsal.photo.data.model.StoryComicPanel
import com.asoft.artsal.photo.data.model.StoryComicResponse
import com.asoft.artsal.photo.extensions.downloadImageAsBitmap
import com.asoft.artsal.photo.extensions.initRecyclerViewAdapter
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.popBackStack
import com.asoft.artsal.photo.extensions.toast
import com.asoft.artsal.photo.ui.ai.adapter.StoryComicResultAdapter
import com.asoft.artsal.photo.ui.ai.viewmodel.ImageViewModel
import com.asoft.artsal.photo.utils.Const
import com.asoft.artsal.photo.utils.StorageUtils.saveImageToStorage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class ResultRenerateFragment : BaseFragment<FragmentResultRenerateBinding>() {
    override val isLightStatusBar: Boolean
        get() = false

    private val viewModel: ImageViewModel by activityViewModels()

    private val resultAdapter by lazy {
        StoryComicResultAdapter(
            onItemClick = { page ->
                requireContext().toast("Clicked page ${page.pageIndex + 1}")
            }
        )
    }

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentResultRenerateBinding {
        return FragmentResultRenerateBinding.inflate(inflater, container, false)
    }

    override fun initListener() {
        binding?.apply {
            btnBack.onClick {
                viewModel.clearStoryComicResult()
                viewModel.clearComicResult()
                popBackStack()
            }

            btnDownload.onClick {
                downloadAllImages()
            }
        }
    }

    override fun setupUi() {
        setupRecyclerView()
        loadData()
    }

    private fun setupRecyclerView() {
        binding?.rvResults?.initRecyclerViewAdapter(
            yourAdapter = resultAdapter,
            yourLayoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
            ),
            fixedSize = false
        )
    }

    private fun loadData() {
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    viewModel.storyComicResult.collect { result ->
                        if (result != null) {
                            Timber.d("ResultRenerateFragment: Received StoryComic result with ${result.pages.size} pages")
                            displayStoryComicResult(result)
                        } else {
                            Timber.d("ResultRenerateFragment: No StoryComic result available")
                        }
                    }
                }
                launch {
                    viewModel.comicResult.collect { result ->
                        if (result != null) {
                            Timber.d("ResultRenerateFragment: Received Comic result with ${result.data.panels.size} panels")
                            displayComicResult(result)
                        } else {
                            Timber.d("ResultRenerateFragment: No Comic result available")
                        }
                    }
                }
            }
        }
    }

    private fun displayStoryComicResult(response: StoryComicResponse) {
        binding?.apply {
            Timber.d("ResultRenerateFragment: Displaying result - ${response.pages.size} pages")

            if (response.pages.isEmpty()) {
                Timber.w("ResultRenerateFragment: No pages in response")
                toast("Không có dữ liệu để hiển thị")
                return
            }

            // Update title with page count
            tvResultTitle.text = "Kết quả (${response.pages.size} Trang)"

            // Create map of page titles from meta outline if available
            val pageTitles = response.meta.outline.associate { outline ->
                outline.id to outline.summaryVi.take(30)
            }

            // Log first page URL for debugging
            response.pages.firstOrNull()?.let { firstPage ->
                Timber.d("ResultRenerateFragment: First page URL: ${firstPage.presignedUrl ?: firstPage.pageUrl}")
            }

            // Create new adapter with page titles
            val adapter = StoryComicResultAdapter(
                onItemClick = { page ->
                    requireContext().toast("Clicked page ${page.pageIndex + 1}")
                },
                pageTitles = pageTitles
            )

            // Setup RecyclerView with new adapter
            rvResults.adapter = adapter

            // Submit pages to adapter
            adapter.submitList(response.pages)

            Timber.d("ResultRenerateFragment: Adapter set with ${response.pages.size} items")
        }
    }

    private fun displayComicResult(response: ComicResponse) {
        binding?.apply {
            Timber.d("ResultRenerateFragment: Displaying Comic result - ${response.meta.pages} pages")

            // Convert ComicResponse to list of StoryComicPage for display
            val pages = convertComicResponseToPages(response)

            if (pages.isEmpty()) {
                Timber.w("ResultRenerateFragment: No pages in Comic response")
                toast("Không có dữ liệu để hiển thị")
                return
            }

            // Update title with page count
            tvResultTitle.text = getString(R.string.result_pages, pages.size)

            // Create map of page titles from panel descriptions
            val pageTitles = pages.associate { page ->
                page.pageIndex to (page.panels.firstOrNull()?.dialogue?.take(30)
                    ?: "Trang ${page.pageIndex + 1}")
            }

            // Log first page URL for debugging
            pages.firstOrNull()?.let { firstPage ->
                Timber.d("ResultRenerateFragment: First page URL: ${firstPage.presignedUrl ?: firstPage.pageUrl}")
            }

            // Create new adapter with page titles
            val adapter = StoryComicResultAdapter(
                onItemClick = { page ->
                    requireContext().toast("Clicked page ${page.pageIndex + 1}")
                },
                pageTitles = pageTitles
            )

            // Setup RecyclerView with new adapter
            rvResults.adapter = adapter

            // Submit pages to adapter
            adapter.submitList(pages)
            viewModel.clearComicResult()


            Timber.d("ResultRenerateFragment: Adapter set with ${pages.size} items")
        }
    }

    /**
     * Convert ComicResponse to list of StoryComicPage for display
     * ComicResponse has a single image with multiple panels grouped by page
     */
    private fun convertComicResponseToPages(response: ComicResponse): List<StoryComicPage> {
        val imageUrl = response.data.image.presignedUrl.ifBlank { response.data.image.url }
        val panelsByPage = response.data.panels.groupBy { it.page }

        return panelsByPage.map { (pageNumber, panels) ->
            StoryComicPage(
                pageIndex = pageNumber - 1, // Convert to 0-based index
                pageUrl = imageUrl,
                key = response.data.image.key,
                presignedUrl = response.data.image.presignedUrl.ifBlank { null },
                panels = panels.map { panel ->
                    StoryComicPanel(
                        id = panel.panel,
                        dialogue = panel.dialogue ?: panel.descriptionVi,
                        speaker = panel.speaker,
                        emotion = panel.emotion
                    )
                }
            )
        }.sortedBy { it.pageIndex }
    }

    private fun downloadAllImages() {
        // Check for StoryComic result first
        val storyComicResponse = viewModel.storyComicResult.value
        if (storyComicResponse != null && storyComicResponse.pages.isNotEmpty()) {
            downloadStoryComicImages(storyComicResponse)
            return
        }

        // Check for Comic result
        val comicResponse = viewModel.comicResult.value
        if (comicResponse != null) {
            downloadComicImages(comicResponse)
            return
        }

        toast("Không có ảnh để tải xuống")
        Timber.w("downloadAllImages: No response available")
    }

    private fun downloadStoryComicImages(response: StoryComicResponse) {

        Timber.d("downloadAllImages: Starting download for ${response.pages.size} pages")

        viewLifecycleOwner.lifecycleScope.launch {
            showLoading(true)
            try {
                val context = requireContext()
                val folder = "/${Const.APP_FOLDER}/${Const.STORY_COMIC_FOLDER}"
                var successCount = 0
                var failCount = 0

                // Download all images in parallel
                val downloadJobs = response.pages.mapIndexed { index, page ->
                    async(Dispatchers.IO) {
                        try {
                            val imageUrl = page.presignedUrl ?: page.pageUrl
                            if (imageUrl.isBlank()) {
                                Timber.w("Page ${page.pageIndex + 1}: Empty URL")
                                return@async false
                            }

                            Timber.d("Page ${page.pageIndex + 1}: Downloading from URL: $imageUrl")

                            // Download bitmap from URL (this will switch to Main thread internally)
                            val bitmap = downloadImageAsBitmap(context, imageUrl)
                            if (bitmap == null || bitmap.isRecycled) {
                                Timber.e("Page ${page.pageIndex + 1}: Failed to download bitmap or bitmap is recycled")
                                return@async false
                            }

                            Timber.d("Page ${page.pageIndex + 1}: Downloaded bitmap successfully, size: ${bitmap.width}x${bitmap.height}")

                            // Generate filename with story ID if available
                            val storyId = response.storyId?.take(8) ?: ""
                            val timestamp = System.currentTimeMillis()
                            val fileName = if (storyId.isNotEmpty()) {
                                "story_comic_${storyId}_page_${page.pageIndex + 1}_${timestamp}"
                            } else {
                                "story_comic_page_${page.pageIndex + 1}_${timestamp}"
                            }

                            // Save to storage
                            // Note: saveImageToStorage uses ContentResolver which is thread-safe
                            val result = saveImageToStorage(
                                context = context,
                                pathName = folder,
                                bitmap = bitmap,
                                fileName = fileName
                            )

                            if (result) {
                                Timber.d("Page ${page.pageIndex + 1}: Saved successfully to $folder/$fileName")
                            } else {
                                Timber.e("Page ${page.pageIndex + 1}: Failed to save to storage")
                            }

                            result
                        } catch (e: Exception) {
                            Timber.e(e, "Error downloading page ${page.pageIndex + 1}")
                            false
                        }
                    }
                }

                // Wait for all downloads to complete
                val results = downloadJobs.awaitAll()
                results.forEachIndexed { index, success ->
                    if (success) {
                        successCount++
                        Timber.d("Page ${index + 1}: Download completed successfully")
                    } else {
                        failCount++
                        Timber.w("Page ${index + 1}: Download failed")
                    }
                }

                showLoading(false)

                // Show result message
                when {
                    successCount > 0 && failCount == 0 -> {
                        toast("Đã tải xuống $successCount ảnh thành công")
                        Timber.d("downloadStoryComicImages: All $successCount images downloaded successfully")
                    }

                    successCount > 0 && failCount > 0 -> {
                        toast("Đã tải xuống $successCount ảnh, $failCount ảnh thất bại")
                        Timber.w("downloadStoryComicImages: Partial success - $successCount success, $failCount failed")
                    }

                    else -> {
                        toast("Tải xuống thất bại")
                        Timber.e("downloadStoryComicImages: All downloads failed")
                    }
                }
            } catch (e: Exception) {
                showLoading(false)
                Timber.e(e, "Error downloading StoryComic images")
                toast("Lỗi khi tải xuống: ${e.message}")
            }
        }
    }

    private fun downloadComicImages(response: ComicResponse) {
        viewLifecycleOwner.lifecycleScope.launch {
            showLoading(true)
            try {
                val context = requireContext()
                val folder = "/${Const.APP_FOLDER}/${Const.STORY_COMIC_FOLDER}"
                val imageUrl = response.data.image.presignedUrl.ifBlank { response.data.image.url }

                if (imageUrl.isBlank()) {
                    toast("Không có URL ảnh để tải xuống")
                    showLoading(false)
                    return@launch
                }

                Timber.d("downloadComicImages: Downloading single comic image from URL: $imageUrl")

                // Download bitmap from URL
                val bitmap = downloadImageAsBitmap(context, imageUrl)
                if (bitmap == null || bitmap.isRecycled) {
                    Timber.e("downloadComicImages: Failed to download bitmap")
                    toast("Tải xuống thất bại")
                    showLoading(false)
                    return@launch
                }

                Timber.d("downloadComicImages: Downloaded bitmap successfully, size: ${bitmap.width}x${bitmap.height}")

                // Generate filename
                val comicId = response.data.comicId.take(8)
                val timestamp = System.currentTimeMillis()
                val fileName = "comic_${comicId}_${timestamp}"

                // Save to storage
                val result = saveImageToStorage(
                    context = context,
                    pathName = folder,
                    bitmap = bitmap,
                    fileName = fileName
                )

                showLoading(false)

                if (result) {
                    toast("Đã tải xuống ảnh thành công")
                    Timber.d("downloadComicImages: Saved successfully to $folder/$fileName")
                } else {
                    toast("Tải xuống thất bại")
                    Timber.e("downloadComicImages: Failed to save to storage")
                }
            } catch (e: Exception) {
                showLoading(false)
                Timber.e(e, "Error downloading Comic image")
                toast("Lỗi khi tải xuống: ${e.message}")
            }
        }
    }

    override fun renderUi() {
        // handled in setupUi
    }
}

