package com.asoft.artsal.photo.ui.ai.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.artsal.photo.editor.collage.maker.databinding.ItemResultComicBinding
import com.asoft.artsal.photo.data.model.StoryComicPage
import com.asoft.artsal.photo.extensions.inflater
import com.asoft.artsal.photo.extensions.loadImageFromUrlWithLoading

class StoryComicResultAdapter(
    private val onItemClick: (StoryComicPage) -> Unit,
    private val pageTitles: Map<Int, String> = emptyMap() // Map of pageIndex to title
) : ListAdapter<StoryComicPage, StoryComicResultAdapter.StoryComicResultViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): StoryComicResultViewHolder {
        val binding = ItemResultComicBinding.inflate(parent.inflater, parent, false)
        return StoryComicResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StoryComicResultViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StoryComicResultViewHolder(
        private val binding: ItemResultComicBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(page: StoryComicPage) {
            binding.apply {
                // Set page number
//                tvPageNumber.text = "${page.pageIndex + 1}"
                
                // Set page title - prefer from pageTitles map, then first panel dialogue, then default
//                val pageTitle = when {
//                    pageTitles.containsKey(page.pageIndex) -> pageTitles[page.pageIndex] ?: ""
//                    page.panels.isNotEmpty() && !page.panels.first().dialogue.isNullOrBlank() -> {
//                        page.panels.first().dialogue?.take(30) ?: "Trang ${page.pageIndex + 1}"
//                    }
//                    else -> "Trang ${page.pageIndex + 1}"
//                }
//                tvPageTitle.text = pageTitle
                
                // Set panel count
//                tvPanelCount.text = "${page.panels.size} PANELS"
                
                // Load image from URL (prefer presignedUrl, fallback to pageUrl)
                val imageUrl = page.presignedUrl ?: page.pageUrl
                if (imageUrl.isNotBlank()) {
                    imgComicPage.loadImageFromUrlWithLoading(imageUrl)
                } else {
                    imgComicPage.setImageResource(com.artsal.photo.editor.collage.maker.R.drawable.bg_error_img)
                }
                root.setOnClickListener {
                    onItemClick(page)
                }
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<StoryComicPage>() {
        override fun areItemsTheSame(oldItem: StoryComicPage, newItem: StoryComicPage): Boolean {
            return oldItem.pageIndex == newItem.pageIndex && oldItem.key == newItem.key
        }

        override fun areContentsTheSame(oldItem: StoryComicPage, newItem: StoryComicPage): Boolean {
            return oldItem == newItem
        }
    }
}

